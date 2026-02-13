package com.carrental.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OwnerPayout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bookingId;
    private Long ownerId;
    private String ownerEmail;
    private BigDecimal totalBookingAmount;
    private BigDecimal ownerShare; // 94%
    private BigDecimal adminShare; // 6%

    @Enumerated(EnumType.STRING)
    private PayoutStatus status;
    private LocalDateTime processedAt;
}