package com.carrental.paymentservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitiatePaymentRequest {
    private Long bookingId;
    private BigDecimal amount; // in rupees
}
