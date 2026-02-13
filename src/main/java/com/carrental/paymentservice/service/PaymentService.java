package com.carrental.paymentservice.service;

import com.carrental.paymentservice.dto.InitiatePaymentRequest;
import com.carrental.paymentservice.dto.InitiatePaymentResponse;
import com.carrental.paymentservice.dto.VerifyPaymentRequest;

import java.math.BigDecimal;

public interface PaymentService {
    InitiatePaymentResponse initiatePayment(InitiatePaymentRequest req) throws Exception;
    void verifyPayment(VerifyPaymentRequest req) throws Exception;
    void handleWebhook(String payload, String razorpaySignature) throws Exception;
    void processOwnerSettlement(Long bookingId, Long ownerId, String ownerEmail, BigDecimal totalAmount);
    void processRefund(Long userId, String email, BigDecimal amount, Long bookingId);

}
