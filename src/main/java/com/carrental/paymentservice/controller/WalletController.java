package com.carrental.paymentservice.controller;

import com.carrental.paymentservice.dto.BankDetailsDTO;
import com.carrental.paymentservice.dto.WalletResponse;
import com.carrental.paymentservice.dto.WalletTransactionDTO;
import com.carrental.paymentservice.entity.OwnerWallet;
import com.carrental.paymentservice.repository.OwnerWalletRepository;
import com.carrental.paymentservice.repository.WalletTransactionRepository;
import com.carrental.paymentservice.service.PaymentService;
import com.carrental.paymentservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/payments/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletTransactionRepository transactionRepository;
    private final OwnerWalletRepository walletRepository;
    private final PaymentService paymentService;
    private final WalletService walletService;

    // ===============================
    // OWNER ENDPOINTS
    // ===============================

    @GetMapping("/balance/{ownerEmail:.+}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String ownerEmail) {
        return walletRepository.findByOwnerEmail(ownerEmail)
                .map(wallet -> ResponseEntity.ok(wallet.getBalance()))
                .orElse(ResponseEntity.ok(BigDecimal.ZERO));
    }

    @GetMapping("/summary/{ownerEmail:.+}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<WalletResponse> getWalletSummary(@PathVariable String ownerEmail) {

        OwnerWallet wallet = walletRepository.findByOwnerEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Wallet not found for owner: " + ownerEmail));

        List<WalletTransactionDTO> history = transactionRepository
                .findByOwnerEmailOrderByCreatedAtDesc(ownerEmail)
                .stream()
                .map(tx -> new WalletTransactionDTO(
                        tx.getId(),
                        tx.getAmount(),
                        tx.getType(),
                        tx.getDescription(),
                        tx.getCreatedAt()))
                .toList();

        return ResponseEntity.ok(WalletResponse.builder()
                .ownerEmail(ownerEmail)
                .currentBalance(wallet.getBalance())
                .recentTransactions(history)
                .accountNumber(wallet.getAccountNumber())
                .ifscCode(wallet.getIfscCode())
                .build());
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<String> withdrawMoney(
            @RequestParam String ownerEmail,
            @RequestParam BigDecimal amount) {

        walletService.requestWithdrawal(ownerEmail, amount);
        return ResponseEntity.ok("Withdrawal successful! Amount will be credited to your bank soon.");
    }

    @PutMapping("/update-bank")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<String> updateBankDetails(
            @RequestParam String ownerEmail,
            @RequestBody BankDetailsDTO bankDetailsDTO) {

        walletService.updateBankDetails(ownerEmail, bankDetailsDTO);
        return ResponseEntity.ok("Bank details updated successfully!");
    }

    // ===============================
    // INTERNAL ENDPOINTS
    // ===============================

    @PostMapping("/internal/credit")
    public ResponseEntity<Void> creditOnOtp(
            @RequestHeader("X-Internal-Call") String internalHeader,
            @RequestParam Long ownerId,
            @RequestParam String ownerEmail,
            @RequestParam BigDecimal amount,
            @RequestParam String bookingId) {

        if (!"true".equals(internalHeader)) {
            return ResponseEntity.status(403).build();
        }

        paymentService.processOwnerSettlement(
                Long.parseLong(bookingId),
                ownerId,
                ownerEmail,
                amount
        );
        return ResponseEntity.ok().build();
    }

    // ===============================
    // PUBLIC (INTERNAL USE BY USER SERVICE)
    // ===============================

    @PostMapping("/create")
    public ResponseEntity<String> createWallet(
            @RequestParam String ownerEmail,
            @RequestParam Long ownerId) {

        if (walletRepository.existsByOwnerEmail(ownerEmail)) {
            return ResponseEntity.ok("Wallet already exists for user: " + ownerEmail);
        }

        OwnerWallet wallet = new OwnerWallet();
        wallet.setOwnerEmail(ownerEmail);
        wallet.setOwnerId(ownerId);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setUpdatedAt(LocalDateTime.now());

        walletRepository.save(wallet);
        return ResponseEntity.ok("Wallet created successfully for: " + ownerEmail);
    }
}
