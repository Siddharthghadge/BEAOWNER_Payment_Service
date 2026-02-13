package com.carrental.paymentservice.dto;

import com.carrental.paymentservice.entity.WalletTransaction;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class WalletSummaryDTO {
    private BigDecimal currentBalance;
    private List<WalletTransaction> recentTransactions;
    private String accountNumber;
    private String ifscCode;
    private String accountHolderName;
}