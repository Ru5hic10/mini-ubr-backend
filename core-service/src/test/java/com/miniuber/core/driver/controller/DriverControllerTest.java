package com.miniuber.core.driver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniuber.core.driver.dto.DriverAvailabilityRequest;
import com.miniuber.core.driver.dto.DriverLocationUpdateRequest;
import com.miniuber.core.driver.dto.DriverRegistrationRequest;
import com.miniuber.core.driver.dto.DriverResponse;
import com.miniuber.core.driver.service.DriverService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DriverController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class DriverControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private DriverService driverService;

        @MockBean
        private com.miniuber.core.auth.util.JwtUtil jwtUtil;

        @Test
        void registerDriver_Success() throws Exception {
                DriverRegistrationRequest request = new DriverRegistrationRequest();
                request.setName("Test Driver");
                request.setEmail("driver@example.com");
                request.setPassword("password");
                request.setPhone("1234567890");
                request.setLicenseNumber("LIC123");
                request.setVehicleType("Sedan");
                request.setVehicleNumber("KA01AB1234");

                DriverResponse response = new DriverResponse(1L, "Test Driver", "driver@example.com", "1234567890",
                                "LIC123",
                                "Sedan", "V123", "Model", false, false, 0.0, 0, null);

                when(driverService.registerDriver(any(DriverRegistrationRequest.class))).thenReturn(response);

                mockMvc.perform(post("/api/drivers/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.email").value("driver@example.com"));
        }

        @Test
        void updateAvailability_Success() throws Exception {
                DriverAvailabilityRequest request = new DriverAvailabilityRequest();
                request.setAvailable(true);

                DriverResponse response = new DriverResponse(1L, "Test Driver", "driver@example.com", "1234567890",
                                "LIC123",
                                "Sedan", "V123", "Model", true, true, 0.0, 0, null);

                when(driverService.updateAvailability(eq(1L), any(DriverAvailabilityRequest.class)))
                                .thenReturn(response);

                mockMvc.perform(put("/api/drivers/1/availability")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.available").value(true));
        }

        @Test
        void updateLocation_Success() throws Exception {
                DriverLocationUpdateRequest request = new DriverLocationUpdateRequest();
                request.setLatitude(12.9716);
                request.setLongitude(77.5946);

                DriverResponse response = new DriverResponse(1L, "Test Driver", "driver@example.com", "1234567890",
                                "LIC123",
                                "Sedan", "V123", "Model", true, true, 0.0, 0, null);

                when(driverService.updateLocation(eq(1L), any(DriverLocationUpdateRequest.class))).thenReturn(response);

                mockMvc.perform(put("/api/drivers/1/location")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        void getAvailableDrivers_Success() throws Exception {
                DriverResponse response = new DriverResponse(1L, "Test Driver", "driver@example.com", "1234567890",
                                "LIC123",
                                "Sedan", "V123", "Model", true, true, 0.0, 0, null);
                when(driverService.getAvailableDrivers()).thenReturn(Collections.singletonList(response));

                mockMvc.perform(get("/api/drivers/available"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        void updateRating_Success() throws Exception {
                Map<String, Double> request = Map.of("rating", 4.5);
                DriverResponse response = new DriverResponse(1L, "Test Driver", "driver@example.com", "1234567890",
                                "LIC123",
                                "Sedan", "V123", "Model", true, true, 4.5, 0, null);

                when(driverService.updateRating(eq(1L), eq(4.5))).thenReturn(response);

                mockMvc.perform(put("/api/drivers/1/rating")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.rating").value(4.5));
        }
}
