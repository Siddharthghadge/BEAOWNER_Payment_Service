package com.carrental.paymentservice.dto;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class WithdrawalRequest {
    private BigDecimal amount;
    private String bankAccountNumber;
    private String ifscCode;
}