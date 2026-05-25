package com.miniuber.payment.repository;

import com.miniuber.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRideId(Long rideId);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByRiderIdOrderByCreatedAtDesc(Long riderId);

    List<Payment> findAllByOrderByCreatedAtDesc();
}
