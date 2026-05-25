package com.miniuber.payment.service;

import com.miniuber.payment.dto.PaymentRequest;
import com.miniuber.payment.entity.Payment;
import com.miniuber.payment.entity.PaymentStatus;
import com.miniuber.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setRideId(1L);
        testPayment.setAmount(15.50);
        testPayment.setPaymentMethod("CREDIT_CARD");
        testPayment.setStatus(PaymentStatus.COMPLETED);
        testPayment.setTransactionId("txn-123");
    }

    @Test
    void processPayment_Success() {
        PaymentRequest request = new PaymentRequest();
        request.setRideId(1L);
        request.setAmount(15.50);
        request.setPaymentMethod("CREDIT_CARD");

        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        Payment result = paymentService.processPayment(request);

        assertNotNull(result);
        assertEquals(1L, result.getRideId());
        assertEquals(15.50, result.getAmount());
        assertEquals("CREDIT_CARD", result.getPaymentMethod());
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getTransactionId());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void processPayment_DifferentPaymentMethod() {
        PaymentRequest request = new PaymentRequest();
        request.setRideId(2L);
        request.setAmount(25.00);
        request.setPaymentMethod("CASH");

        Payment cashPayment = new Payment();
        cashPayment.setRideId(2L);
        cashPayment.setAmount(25.00);
        cashPayment.setPaymentMethod("CASH");
        cashPayment.setStatus(PaymentStatus.COMPLETED);

        when(paymentRepository.save(any(Payment.class))).thenReturn(cashPayment);

        Payment result = paymentService.processPayment(request);

        assertNotNull(result);
        assertEquals("CASH", result.getPaymentMethod());
        assertEquals(25.00, result.getAmount());
    }

    @Test
    void getPaymentByRideId_Success() {
        when(paymentRepository.findByRideId(1L)).thenReturn(Optional.of(testPayment));

        Payment result = paymentService.getPaymentByRideId(1L);

        assertNotNull(result);
        assertEquals(1L, result.getRideId());
        assertEquals(15.50, result.getAmount());
    }

    @Test
    void getPaymentByRideId_NotFound() {
        when(paymentRepository.findByRideId(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            paymentService.getPaymentByRideId(1L);
        });

        assertEquals("Payment not found for ride id: 1", exception.getMessage());
    }
}
