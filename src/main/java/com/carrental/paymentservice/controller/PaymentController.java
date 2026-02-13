package com.carrental.paymentservice.controller;

import com.carrental.paymentservice.dto.InitiatePaymentRequest;
import com.carrental.paymentservice.dto.InitiatePaymentResponse;
import com.carrental.paymentservice.dto.VerifyPaymentRequest;
import com.carrental.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;


@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ROLE_CUSTOMER', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<InitiatePaymentResponse> initiatePayment(
            @RequestBody InitiatePaymentRequest request
    ) throws Exception {
        InitiatePaymentResponse res = paymentService.initiatePayment(request);
        return ResponseEntity.ok(res);
    }

    // Client can call this after Checkout (optional) — we recommend using webhook instead
    @PostMapping("/verify")
    public ResponseEntity<String> verify(@RequestBody VerifyPaymentRequest req) throws Exception {
        paymentService.verifyPayment(req);
        return ResponseEntity.ok("Payment verified and booking confirmed");
    }

    @PostMapping("/refund")
    public ResponseEntity<String> refund(
            @RequestParam Long userId,
            @RequestParam String email,
            @RequestParam BigDecimal amount,   // ✅ BigDecimal
            @RequestParam Long bookingId
    ) {
        paymentService.processRefund(userId, email, amount, bookingId);
        return ResponseEntity.ok("Refund successful");
    }



}
