package com.carrental.paymentservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Builder // ✅ ENSURE THIS IS HERE
@AllArgsConstructor // ✅ REQUIRED FOR BUILDER
@NoArgsConstructor
@Getter @Setter
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long ownerId;
    private String ownerEmail;
    private BigDecimal amount;
    private String type; // "CREDIT" or "DEBIT"
    private String description;
    private LocalDateTime createdAt;
}
