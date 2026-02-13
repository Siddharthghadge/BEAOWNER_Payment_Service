package com.carrental.paymentservice.repository;

import com.carrental.paymentservice.entity.Payment;
import com.carrental.paymentservice.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    Optional<Payment> findByBookingId(Long bookingId);
    Optional<Payment> findTopByBookingIdAndStatusOrderByIdDesc(Long bookingId, PaymentStatus status);

}
