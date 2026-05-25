package com.miniuber.core.driver.service;

import com.miniuber.core.driver.client.RideServiceClient;
import com.miniuber.core.driver.dto.DriverAvailabilityRequest;
import com.miniuber.core.driver.dto.DriverRegistrationRequest;
import com.miniuber.core.driver.dto.DriverResponse;
import com.miniuber.core.driver.entity.Driver;
import com.miniuber.core.driver.repository.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private RideServiceClient rideServiceClient;

    @InjectMocks
    private DriverService driverService;

    private Driver testDriver;

    @BeforeEach
    void setUp() {
        testDriver = new Driver();
        testDriver.setId(10L);
        testDriver.setName("Test Driver");
        testDriver.setEmail("driver@example.com");
        testDriver.setPassword(new BCryptPasswordEncoder().encode("password"));
        testDriver.setLicenseNumber("LIC-123");
        testDriver.setAvailable(false);
        testDriver.setVerified(false);
    }

    @Test
    void registerDriver_Success() {
        DriverRegistrationRequest request = new DriverRegistrationRequest();
        request.setName("Test Driver");
        request.setEmail("driver@example.com");
        request.setPassword("password");
        request.setLicenseNumber("LIC-123");
        request.setVehicleType("SUV");

        when(driverRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(driverRepository.existsByLicenseNumber(request.getLicenseNumber())).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenAnswer(i -> {
            Driver d = i.getArgument(0);
            d.setId(10L);
            return d;
        });

        DriverResponse response = driverService.registerDriver(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("driver@example.com", response.getEmail());
        verify(driverRepository, times(1)).save(any(Driver.class));
    }

    @Test
    void updateAvailability_Success() {
        DriverAvailabilityRequest request = new DriverAvailabilityRequest();
        request.setAvailable(true);

        when(driverRepository.findById(10L)).thenReturn(Optional.of(testDriver));
        when(driverRepository.save(any(Driver.class))).thenReturn(testDriver);

        DriverResponse response = driverService.updateAvailability(10L, request);

        assertTrue(testDriver.getAvailable());
        assertTrue(testDriver.getVerified()); // Logic says setting available=true sets verified=true
        verify(driverRepository, times(1)).save(testDriver);
    }

    @Test
    void getEarningsSummary_Success() {
        when(driverRepository.findById(10L)).thenReturn(Optional.of(testDriver));
        when(rideServiceClient.getDriverTotalEarnings(10L)).thenReturn(500.0);
        when(rideServiceClient.getDriverCompletedRidesCount(10L)).thenReturn(5);

        Map<String, Object> summary = driverService.getEarningsSummary(10L);

        assertEquals(500.0, summary.get("totalEarnings"));
        assertEquals(5, summary.get("totalRides"));
        assertEquals(100.0, summary.get("averagePerRide"));
    }
}
