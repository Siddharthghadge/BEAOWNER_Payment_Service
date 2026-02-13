package com.carrental.paymentservice.repository;

import com.carrental.paymentservice.entity.OwnerWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OwnerWalletRepository extends JpaRepository<OwnerWallet, Long> {

    /**
     * Finds wallet using the numeric Owner ID.
     */
    Optional<OwnerWallet> findByOwnerId(Long ownerId);

    /**
     * Finds wallet using the Owner's Email address.
     * This matches the change we made in the Controller to handle
     * the 'siddharthghadge321@gmail.com' string from the frontend.
     */

    @Query("SELECT w FROM OwnerWallet w WHERE w.ownerEmail = :email")
    Optional<OwnerWallet> findByOwnerEmail(@Param("email") String email);

    boolean existsByOwnerEmail(String email);
}