package com.carrental.paymentservice.entity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity
@Data
public class OwnerWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ownerId;
    private String ownerEmail;
    private BigDecimal balance = BigDecimal.ZERO;

    // --- NEW BANK DETAIL FIELDS ---
    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private String bankName; // Optional (e.g., HDFC, SBI)

    // --- RAZORPAYX SPECIFIC (For Step 2) ---
    private String rzpContactId;     // To identify the person in Razorpay
    private String rzpFundAccountId; // To identify the specific bank account in Razorpay

    private LocalDateTime updatedAt;
}