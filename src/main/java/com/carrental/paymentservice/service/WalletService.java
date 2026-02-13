package com.carrental.paymentservice.service;

import com.carrental.paymentservice.dto.BankDetailsDTO;
import com.carrental.paymentservice.dto.WalletSummaryDTO;
import com.carrental.paymentservice.entity.OwnerWallet;
import com.carrental.paymentservice.entity.WalletTransaction;
import com.carrental.paymentservice.repository.OwnerWalletRepository;
import com.carrental.paymentservice.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final OwnerWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final RazorpayPayoutService payoutService;

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    // ===============================
    // CREDIT WALLET (OWNER SETTLEMENT)
    // ===============================
    @Transactional
    public void creditWallet(String ownerEmail, BigDecimal amount, String description) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Credit amount must be greater than zero");
        }

        OwnerWallet wallet = walletRepository.findByOwnerEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Wallet not found for: " + ownerEmail));

        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        transactionRepository.save(WalletTransaction.builder()
                .ownerEmail(ownerEmail)
                .amount(amount.setScale(2, RoundingMode.HALF_UP))
                .type("CREDIT")
                .description(description)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // ===============================
    // WITHDRAWAL (BANK PAYOUT)
    // ===============================
    @Transactional
    public void requestWithdrawal(String ownerEmail, BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Withdrawal amount must be greater than zero");
        }

        OwnerWallet wallet = walletRepository.findByOwnerEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        if (wallet.getAccountNumber() == null || wallet.getIfscCode() == null) {
            throw new RuntimeException("Please link bank account before withdrawal");
        }

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient wallet balance");
        }

        try {
            payoutService.createPayout(
                    wallet.getAccountHolderName(),
                    wallet.getOwnerEmail(),
                    wallet.getAccountNumber(),
                    wallet.getIfscCode(),
                    amount
            );
        } catch (Exception e) {
            log.error("Bank payout failed", e);
            throw new RuntimeException("Bank gateway error");
        }

        wallet.setBalance(
                wallet.getBalance().subtract(amount).setScale(2, RoundingMode.HALF_UP)
        );
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        transactionRepository.save(WalletTransaction.builder()
                .ownerEmail(ownerEmail)
                .amount(amount.setScale(2, RoundingMode.HALF_UP))
                .type("DEBIT")
                .description("Bank Withdrawal Successful")
                .createdAt(LocalDateTime.now())
                .build());
    }

    // ===============================
    // BANK DETAILS UPDATE
    // ===============================
    @Transactional
    public void updateBankDetails(String email, BankDetailsDTO dto) {

        OwnerWallet wallet = walletRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new RuntimeException("Wallet not found for email: " + email));

        wallet.setAccountHolderName(dto.getAccountHolderName());
        wallet.setAccountNumber(dto.getAccountNumber());
        wallet.setIfscCode(dto.getIfscCode());
        wallet.setBankName(dto.getBankName());
        wallet.setUpdatedAt(LocalDateTime.now());

        walletRepository.save(wallet);
    }

    // ===============================
    // WALLET SUMMARY
    // ===============================
    public WalletSummaryDTO getWalletSummary(String email) {

        OwnerWallet wallet = walletRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        List<WalletTransaction> transactions =
                transactionRepository.findByOwnerEmail(email);

        WalletSummaryDTO summary = new WalletSummaryDTO();
        summary.setCurrentBalance(wallet.getBalance());
        summary.setRecentTransactions(transactions);
        summary.setAccountNumber(wallet.getAccountNumber());
        summary.setIfscCode(wallet.getIfscCode());
        summary.setAccountHolderName(wallet.getAccountHolderName());

        return summary;
    }
}
