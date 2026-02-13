package com.carrental.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletTransactionDTO {
    private Long id;
    private BigDecimal amount;
    private String type; // "CREDIT" or "DEBIT"
    private String description;
    private LocalDateTime createdAt;
}