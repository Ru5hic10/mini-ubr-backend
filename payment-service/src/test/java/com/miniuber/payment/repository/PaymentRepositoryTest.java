package com.miniuber.payment.repository;

import com.miniuber.payment.entity.Payment;
import com.miniuber.payment.entity.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PaymentRepository paymentRepository;

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testPayment = new Payment();
        testPayment.setRideId(1L);
        testPayment.setAmount(15.50);
        testPayment.setPaymentMethod("CREDIT_CARD");
        testPayment.setStatus(PaymentStatus.COMPLETED);
        testPayment.setTransactionId("txn-123");
    }

    @Test
    void savePayment_Success() {
        Payment saved = paymentRepository.save(testPayment);

        assertNotNull(saved.getId());
        assertEquals(1L, saved.getRideId());
        assertEquals(15.50, saved.getAmount());
        assertEquals(PaymentStatus.COMPLETED, saved.getStatus());
        assertNotNull(saved.getTimestamp());
    }

    @Test
    void findByRideId_Success() {
        entityManager.persist(testPayment);
        entityManager.flush();

        Optional<Payment> found = paymentRepository.findByRideId(1L);

        assertTrue(found.isPresent());
        assertEquals(1L, found.get().getRideId());
        assertEquals(15.50, found.get().getAmount());
    }

    @Test
    void findByRideId_NotFound() {
        Optional<Payment> found = paymentRepository.findByRideId(999L);

        assertFalse(found.isPresent());
    }

    @Test
    void findById_Success() {
        entityManager.persist(testPayment);
        entityManager.flush();

        Long id = testPayment.getId();
        Optional<Payment> found = paymentRepository.findById(id);

        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());
    }

    @Test
    void updatePayment_Success() {
        entityManager.persist(testPayment);
        entityManager.flush();

        Payment existing = paymentRepository.findByRideId(1L).orElseThrow();
        existing.setStatus(PaymentStatus.FAILED);
        
        Payment updated = paymentRepository.save(existing);

        assertEquals(PaymentStatus.FAILED, updated.getStatus());
    }

    @Test
    void multiplePayments_DifferentRides() {
        Payment payment1 = new Payment();
        payment1.setRideId(1L);
        payment1.setAmount(10.00);
        payment1.setPaymentMethod("CASH");
        payment1.setStatus(PaymentStatus.COMPLETED);

        Payment payment2 = new Payment();
        payment2.setRideId(2L);
        payment2.setAmount(20.00);
        payment2.setPaymentMethod("CREDIT_CARD");
        payment2.setStatus(PaymentStatus.COMPLETED);

        entityManager.persist(payment1);
        entityManager.persist(payment2);
        entityManager.flush();

        Optional<Payment> found1 = paymentRepository.findByRideId(1L);
        Optional<Payment> found2 = paymentRepository.findByRideId(2L);

        assertTrue(found1.isPresent());
        assertTrue(found2.isPresent());
        assertEquals(10.00, found1.get().getAmount());
        assertEquals(20.00, found2.get().getAmount());
    }
}
