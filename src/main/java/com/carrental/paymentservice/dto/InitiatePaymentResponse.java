package com.carrental.paymentservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitiatePaymentResponse {
    private String razorpayOrderId;
    private String keyId; // razorpay key id for frontend
    private Long bookingId;
    private Long amountInPaise;
    private String currency;
}
