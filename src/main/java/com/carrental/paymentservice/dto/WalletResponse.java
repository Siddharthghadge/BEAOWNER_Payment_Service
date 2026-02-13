package com.carrental.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WalletResponse {
    private String ownerEmail;
    private BigDecimal currentBalance;
    private List<WalletTransactionDTO> recentTransactions;

    // 🆕 Add these fields so the Builder can find them
    private String accountNumber;
    private String ifscCode;
}