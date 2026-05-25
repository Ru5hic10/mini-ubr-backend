package com.miniuber.trip.ride.service;

import com.miniuber.trip.ride.dto.RideRequest;
import com.miniuber.trip.ride.dto.RideResponse;
import com.miniuber.trip.ride.entity.Ride;
import com.miniuber.trip.ride.entity.RideStatus;
import com.miniuber.trip.ride.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private RideService rideService;

    private Ride testRide;

    @BeforeEach
    void setUp() {
        testRide = new Ride();
        testRide.setId(1L);
        testRide.setRiderId(101L);
        testRide.setStatus(RideStatus.REQUESTED);
        testRide.setPickupLatitude(12.9716);
        testRide.setPickupLongitude(77.5946);
        testRide.setDropoffLatitude(12.2958); // Some distance away
        testRide.setDropoffLongitude(76.6394);
    }

    @Test
    void requestRide_Success() {
        RideRequest request = new RideRequest();
        request.setRiderId(101L);
        request.setPickupLatitude(12.9716);
        request.setPickupLongitude(77.5946);
        request.setDropoffLatitude(12.9816); // Short distance
        request.setDropoffLongitude(77.6046);

        when(rideRepository.save(any(Ride.class))).thenAnswer(i -> {
            Ride r = i.getArgument(0);
            r.setId(1L);
            return r;
        });

        // Mock RestTemplate for enrichWithRiderDetails (called in mapToResponse)
        // It's inside a try-catch, so even if it throws or returns null, it's fine.
        // We can mock it to verify interaction.
        when(restTemplate.getForObject(contains("/api/users/"), eq(Map.class)))
                .thenReturn(Map.of("name", "Rider Test", "phone", "1234567890"));

        RideResponse response = rideService.requestRide(request);

        assertNotNull(response);
        assertEquals(RideStatus.REQUESTED, response.getStatus());
        assertNotNull(response.getPrice());
        assertTrue(response.getPrice() > 0);
        assertEquals("Rider Test", response.getRiderName());
    }

    @Test
    void acceptRide_Success() {
        when(rideRepository.findById(1L)).thenReturn(Optional.of(testRide));
        when(rideRepository.save(any(Ride.class))).thenReturn(testRide);

        // Mock enrich calls
        when(restTemplate.getForObject(contains("/api/drivers/"), eq(Map.class)))
                .thenReturn(Map.of("name", "Driver Test", "rating", 4.8));
        when(restTemplate.getForObject(contains("/api/users/"), eq(Map.class)))
                .thenReturn(Map.of("name", "Rider Test"));

        RideResponse response = rideService.acceptRide(1L, 202L);

        assertEquals(RideStatus.ACCEPTED, response.getStatus());
        assertEquals(202L, testRide.getDriverId());
    }

    @Test
    void completeRide_Success() {
        testRide.setStatus(RideStatus.STARTED);
        testRide.setDriverId(202L);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(testRide));
        when(rideRepository.save(any(Ride.class))).thenReturn(testRide);

        RideResponse response = rideService.completeRide(1L);

        assertEquals(RideStatus.COMPLETED, response.getStatus());

        // Verify we tried to make driver available
        verify(restTemplate, times(1)).put(contains("/api/drivers/202/availability"), any());
    }
}
