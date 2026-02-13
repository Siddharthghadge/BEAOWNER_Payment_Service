package com.carrental.paymentservice.service.impl;

import com.carrental.paymentservice.client.BookingServiceClient;
import com.carrental.paymentservice.dto.InitiatePaymentRequest;
import com.carrental.paymentservice.dto.InitiatePaymentResponse;
import com.carrental.paymentservice.dto.VerifyPaymentRequest;
import com.carrental.paymentservice.entity.OwnerPayout;
import com.carrental.paymentservice.entity.Payment;
import com.carrental.paymentservice.entity.PaymentStatus;
import com.carrental.paymentservice.entity.PayoutStatus;
import com.carrental.paymentservice.repository.OwnerPayoutRepository;
import com.carrental.paymentservice.repository.PaymentRepository;

import com.carrental.paymentservice.service.PaymentService;
import com.carrental.paymentservice.service.RazorpayClientService;
import com.carrental.paymentservice.service.WalletService;
import com.carrental.paymentservice.util.SignatureUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final RazorpayClientService razorpayClient;
    private final PaymentRepository paymentRepository;
    private final BookingServiceClient bookingClient;
    private final OwnerPayoutRepository payoutRepository;
    private final WalletService walletService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${razorpay.keyId}")
    private String razorpayKeyId;

    @Value("${razorpay.keySecret}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhookSecret}")
    private String webhookSecret;

    // ------------------------------------------------------------
    // STEP 1: INITIATE PAYMENT (creates Razorpay order)
    // ------------------------------------------------------------
    @Override
    @Transactional
    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest req) throws Exception {

        long amountInPaise = req.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

        // Create order with receipt = booking_<id>
        Order order = razorpayClient.createOrder(
                amountInPaise,
                "INR",
                "booking_" + req.getBookingId()
        );

        Payment payment = Payment.builder()
                .bookingId(req.getBookingId())
                .amount(req.getAmount())
                .status(PaymentStatus.PENDING)
                .razorpayOrderId(order.get("id"))
                .build();

        paymentRepository.save(payment);

        return InitiatePaymentResponse.builder()
                .razorpayOrderId(order.get("id"))
                .keyId(razorpayKeyId)
                .bookingId(req.getBookingId())
                .amountInPaise(amountInPaise)
                .currency("INR")
                .build();
    }

    // ------------------------------------------------------------
    // STEP 2: MANUAL PAYMENT VERIFICATION (frontend callback)
    // ------------------------------------------------------------
    @Override
    @Transactional
    public void verifyPayment(VerifyPaymentRequest req) throws Exception {

        // Validate signature
        String payload = req.getRazorpayOrderId() + "|" + req.getRazorpayPaymentId();
        String expectedSignature = SignatureUtils.hmacSha256(razorpayKeySecret, payload);

        if (!expectedSignature.equals(req.getRazorpaySignature())) {
            throw new IllegalArgumentException("Invalid payment signature");
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(req.getRazorpayOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        payment.setRazorpayPaymentId(req.getRazorpayPaymentId());
        payment.setRazorpaySignature(req.getRazorpaySignature());
        payment.setStatus(PaymentStatus.SUCCESS);

        paymentRepository.save(payment);

        // INTERNAL CONFIRM BOOKING CALL
        bookingClient.confirmBookingInternal(
                payment.getBookingId(),
                req.getRazorpayPaymentId(),
                "PAYMENT_SERVICE"
        );
    }

    // ------------------------------------------------------------
    // STEP 3: HANDLE RAZORPAY WEBHOOK (auto confirmation)
    // ------------------------------------------------------------
    @Override
    @Transactional
    public void handleWebhook(String payload, String razorpaySignature) throws Exception {

        // Validate signature
        String expected = SignatureUtils.hmacSha256(webhookSecret, payload);
        if (!expected.equals(razorpaySignature)) {
            throw new IllegalArgumentException("Invalid webhook signature");
        }

        JsonNode root = objectMapper.readTree(payload);
        String event = root.path("event").asText();

        // -------------------------------
        // PAYMENT CAPTURED SUCCESS
        // -------------------------------
        if (event.equals("payment.captured")) {

            String paymentId = root.at("/payload/payment/entity/id").asText();
            String orderId = root.at("/payload/payment/entity/order_id").asText();

            Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Payment record missing"));

            payment.setRazorpayPaymentId(paymentId);
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);

            // Auto confirm booking
            bookingClient.confirmBookingInternal(
                    payment.getBookingId(),
                    paymentId,
                    "PAYMENT_SERVICE"
            );
        }

        // -------------------------------
        // PAYMENT FAILED
        // -------------------------------
        else if (event.equals("payment.failed")) {

            String orderId = root.at("/payload/payment/entity/order_id").asText();

            paymentRepository.findByRazorpayOrderId(orderId)
                    .ifPresent(p -> {
                        p.setStatus(PaymentStatus.FAILED);
                        paymentRepository.save(p);
                    });
        }

    }
    @Override
    @Transactional
    public void processRefund(Long userId, String email, BigDecimal amount, Long bookingId) {

        Payment payment = paymentRepository
                .findTopByBookingIdAndStatusOrderByIdDesc(bookingId, PaymentStatus.SUCCESS)
                .orElseThrow(() -> new RuntimeException("No SUCCESS payment found for bookingId: " + bookingId));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new RuntimeException("Already refunded for bookingId: " + bookingId);
        }

        if (payment.getRazorpayPaymentId() == null || payment.getRazorpayPaymentId().isBlank()) {
            throw new RuntimeException("Refund not possible: Razorpay paymentId missing for bookingId: " + bookingId);
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Refund amount must be greater than 0");
        }

        if (payment.getAmount() != null && amount.compareTo(payment.getAmount()) > 0) {
            throw new RuntimeException("Refund amount cannot exceed paid amount: " + payment.getAmount());
        }

        try {
            // ✅ Convert INR -> Paise safely
            int amountInPaise = amount.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();

            System.out.println("✅ Refund Request => bookingId=" + bookingId
                    + ", paymentId=" + payment.getRazorpayPaymentId()
                    + ", refundAmountINR=" + amount
                    + ", refundAmountPaise=" + amountInPaise);

            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            // ✅ Fetch payment status
            com.razorpay.Payment razorpayPayment =
                    razorpayClient.payments.fetch(payment.getRazorpayPaymentId());

            String razorpayStatus = razorpayPayment.toJson().getString("status");
            System.out.println("✅ Razorpay payment status: " + razorpayStatus);

            if (!"captured".equalsIgnoreCase(razorpayStatus)) {
                throw new RuntimeException("Refund not allowed because payment is not captured. Razorpay status: " + razorpayStatus);
            }

            // ✅ Create refund
            org.json.JSONObject request = new org.json.JSONObject();
            request.put("amount", amountInPaise);

            com.razorpay.Refund refund =
                    razorpayClient.payments.refund(payment.getRazorpayPaymentId(), request);

            String refundId = refund.toJson().getString("id");
            String refundStatus = refund.toJson().getString("status");

            System.out.println("✅ Refund Created => refundId=" + refundId + ", refundStatus=" + refundStatus);

            // ✅ update DB
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRazorpayRefundId(refundId);
            payment.setRefundStatus(refundStatus);
            payment.setRefundAmount(amount);
            payment.setRefundedAt(LocalDateTime.now());

            paymentRepository.save(payment);

        } catch (Exception e) {
            throw new RuntimeException("Razorpay refund failed: " + e.getMessage());
        }
    }


    @Override
    @Transactional
    public void processOwnerSettlement(Long bookingId,
                                       Long ownerId,
                                       String ownerEmail,
                                       BigDecimal totalAmount) {

        if (payoutRepository.existsByBookingId(bookingId)) {
            throw new RuntimeException("Payout already processed for booking " + bookingId);
        }

        BigDecimal ownerShare = totalAmount.multiply(new BigDecimal("0.94"))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal adminShare = totalAmount.multiply(new BigDecimal("0.06"))
                .setScale(2, RoundingMode.HALF_UP);

        walletService.creditWallet(
                ownerEmail,
                ownerShare,
                "Earnings for Booking #" + bookingId
        );

        payoutRepository.save(OwnerPayout.builder()
                .bookingId(bookingId)
                .ownerId(ownerId)
                .ownerEmail(ownerEmail)
                .totalBookingAmount(totalAmount)
                .ownerShare(ownerShare)
                .adminShare(adminShare)
                .status(PayoutStatus.SUCCESS)
                .processedAt(LocalDateTime.now())
                .build());
    }


}