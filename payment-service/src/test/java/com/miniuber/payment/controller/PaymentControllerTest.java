package com.miniuber.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniuber.payment.dto.PaymentRequest;
import com.miniuber.payment.entity.Payment;
import com.miniuber.payment.entity.PaymentStatus;
import com.miniuber.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    void processPayment_Success() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setRideId(1L);
        request.setAmount(15.50);
        request.setPaymentMethod("CREDIT_CARD");

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setRideId(1L);
        payment.setAmount(15.50);
        payment.setPaymentMethod("CREDIT_CARD");
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId("txn-123");

        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(payment);

        mockMvc.perform(post("/api/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.rideId").value(1))
                .andExpect(jsonPath("$.amount").value(15.50))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getPaymentByRideId_Success() throws Exception {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setRideId(1L);
        payment.setAmount(15.50);
        payment.setStatus(PaymentStatus.COMPLETED);

        when(paymentService.getPaymentByRideId(1L)).thenReturn(payment);

        mockMvc.perform(get("/api/payments/ride/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.rideId").value(1))
                .andExpect(jsonPath("$.amount").value(15.50));
    }

    @Test
    void health_Success() throws Exception {
        mockMvc.perform(get("/api/payments/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Payment Service is running!"));
    }
}
