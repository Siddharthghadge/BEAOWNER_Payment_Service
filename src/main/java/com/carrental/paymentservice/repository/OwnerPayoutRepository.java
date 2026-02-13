package com.carrental.paymentservice.repository;

import com.carrental.paymentservice.entity.OwnerPayout;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerPayoutRepository extends JpaRepository<OwnerPayout, Long> {
    boolean existsByBookingId(Long bookingId);

}