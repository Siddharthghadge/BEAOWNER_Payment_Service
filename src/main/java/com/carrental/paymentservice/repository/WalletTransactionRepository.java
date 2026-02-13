package com.carrental.paymentservice.repository;

import com.carrental.paymentservice.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    // Helpful for the Owner Dashboard later:
    List<WalletTransaction> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<WalletTransaction> findByOwnerEmailOrderByCreatedAtDesc(String ownerEmail);
    List<WalletTransaction> findByOwnerEmail(String ownerEmail);
}