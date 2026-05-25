package com.miniuber.trip.ride.service;

import com.miniuber.trip.ride.dto.RideRequest;
import com.miniuber.trip.ride.dto.RideResponse;
import com.miniuber.trip.ride.entity.Ride;
import com.miniuber.trip.ride.entity.RideStatus;
import com.miniuber.trip.ride.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;
    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Maximum allowed ride distance in kilometers
    private static final double MAX_RIDE_DISTANCE_KM = 30.0;

    @Transactional
    public RideResponse requestRide(RideRequest request) {
        Ride ride = new Ride();
        ride.setRiderId(request.getRiderId());
        ride.setPickupLocation(request.getPickupLocation());
        ride.setPickupLatitude(request.getPickupLatitude());
        ride.setPickupLongitude(request.getPickupLongitude());
        ride.setDropoffLocation(request.getDropoffLocation());
        ride.setDropoffLatitude(request.getDropoffLatitude());
        ride.setDropoffLongitude(request.getDropoffLongitude());

        // Calculate distance and price
        if (request.getPickupLatitude() != null && request.getPickupLongitude() != null &&
                request.getDropoffLatitude() != null && request.getDropoffLongitude() != null) {
            double distance = calculateDistance(
                    request.getPickupLatitude(), request.getPickupLongitude(),
                    request.getDropoffLatitude(), request.getDropoffLongitude());

            // Validate maximum distance
            if (distance > MAX_RIDE_DISTANCE_KM) {
                throw new IllegalArgumentException(
                        String.format("Ride distance of %.1f km exceeds maximum allowed distance of %.0f km",
                                distance, MAX_RIDE_DISTANCE_KM));
            }

            ride.setDistance(distance);
            ride.setEstimatedDuration((int) (distance * 2)); // Rough estimate: 2 min per km
            ride.setPrice(calculatePrice(distance));
        } else {
            // Fallback if coordinates missing (should be validated)
            ride.setPrice(40.0); // Minimum base fare
        }

        ride.setStatus(RideStatus.REQUESTED);

        Ride savedRide = rideRepository.save(ride);

        // Publish ride request event to Kafka for driver notifications
        // This demonstrates async messaging: RideService -> Kafka ->
        // DriverNotificationListener -> WebSocket
        publishRideRequestToKafka(savedRide);

        return mapToResponse(savedRide);
    }

    /**
     * Publish new ride request to Kafka topic for driver notifications.
     * The DriverNotificationListener will consume this and send WebSocket
     * notifications.
     */
    private void publishRideRequestToKafka(Ride ride) {
        try {
            String message = String.format(
                    "{\"rideId\":%d,\"riderId\":%d,\"pickupLocation\":\"%s\",\"dropoffLocation\":\"%s\"," +
                            "\"pickupLatitude\":%s,\"pickupLongitude\":%s,\"estimatedFare\":%.2f,\"distance\":%.2f}",
                    ride.getId(),
                    ride.getRiderId(),
                    ride.getPickupLocation() != null ? ride.getPickupLocation().replace("\"", "\\\"") : "",
                    ride.getDropoffLocation() != null ? ride.getDropoffLocation().replace("\"", "\\\"") : "",
                    ride.getPickupLatitude() != null ? ride.getPickupLatitude() : 0.0,
                    ride.getPickupLongitude() != null ? ride.getPickupLongitude() : 0.0,
                    ride.getPrice() != null ? ride.getPrice() : 0.0,
                    ride.getDistance() != null ? ride.getDistance() : 0.0);

            kafkaTemplate.send("drivers.notification", message);
            log.info("Published ride request {} to Kafka topic 'drivers.notification'", ride.getId());
        } catch (Exception e) {
            log.error("Failed to publish ride request {} to Kafka: {}", ride.getId(), e.getMessage());
            // Don't fail the ride request if Kafka publish fails
        }
    }

    @Transactional
    public RideResponse updatePaymentStatus(Long rideId, String status) {
        Ride ride = getRideEntity(rideId);
        ride.setPaymentStatus(status);
        return mapToResponse(rideRepository.save(ride));
    }

    @Transactional
    public RideResponse acceptRide(Long rideId, Long driverId) {
        Ride ride = getRideEntity(rideId);
        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new RuntimeException("Ride is not in REQUESTED state");
        }
        ride.setDriverId(driverId);
        ride.setStatus(RideStatus.ACCEPTED);
        return mapToResponse(rideRepository.save(ride));
    }

    @Transactional
    public RideResponse startRide(Long rideId) {
        Ride ride = getRideEntity(rideId);
        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new RuntimeException("Ride is not in ACCEPTED state");
        }
        ride.setStatus(RideStatus.STARTED);
        ride.setStartTime(java.time.LocalDateTime.now());
        return mapToResponse(rideRepository.save(ride));
    }

    @Transactional
    public RideResponse completeRide(Long rideId) {
        Ride ride = getRideEntity(rideId);
        if (ride.getStatus() != RideStatus.STARTED) {
            throw new RuntimeException("Ride is not in STARTED state");
        }
        ride.setStatus(RideStatus.COMPLETED);
        ride.setEndTime(java.time.LocalDateTime.now());

        // Persist completion first
        Ride savedRide = rideRepository.save(ride);

        // Mark driver available again after completion so they stay online
        if (ride.getDriverId() != null) {
            try {
                String url = "http://core-service:8081/api/drivers/" + ride.getDriverId() + "/availability";
                restTemplate.put(url, Map.of("available", true));
            } catch (Exception e) {
                log.warn("Failed to set driver {} availability after completion: {}", ride.getDriverId(),
                        e.getMessage());
            }
        }

        return mapToResponse(savedRide);
    }

    @Transactional
    public RideResponse cancelRide(Long rideId) {
        Ride ride = getRideEntity(rideId);
        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.CANCELLED) {
            throw new RuntimeException("Ride cannot be cancelled");
        }
        ride.setStatus(RideStatus.CANCELLED);
        return mapToResponse(rideRepository.save(ride));
    }

    public RideResponse getRideById(Long id) {
        return mapToResponse(getRideEntity(id));
    }

    public List<RideResponse> getRidesByRider(Long riderId) {
        return rideRepository.findByRiderId(riderId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RideResponse> getRidesByDriver(Long driverId) {
        return rideRepository.findByDriverId(driverId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RideResponse> getAvailableRides() {
        return rideRepository.findByStatus(RideStatus.REQUESTED).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Ride getRideEntity(Long id) {
        return rideRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
    }

    private RideResponse mapToResponse(Ride ride) {
        RideResponse response = RideResponse.builder()
                .id(ride.getId())
                .riderId(ride.getRiderId())
                .driverId(ride.getDriverId())
                .pickupLocation(ride.getPickupLocation())
                .pickupLatitude(ride.getPickupLatitude())
                .pickupLongitude(ride.getPickupLongitude())
                .dropoffLocation(ride.getDropoffLocation())
                .dropoffLatitude(ride.getDropoffLatitude())
                .dropoffLongitude(ride.getDropoffLongitude())
                .distance(ride.getDistance())
                .estimatedDuration(ride.getEstimatedDuration())
                .price(ride.getPrice())
                .status(ride.getStatus())
                .paymentStatus(ride.getPaymentStatus())
                .startTime(ride.getStartTime())
                .endTime(ride.getEndTime())
                .createdAt(ride.getCreatedAt())
                .build();

        // Enrich with driver details if driver is assigned
        if (ride.getDriverId() != null) {
            enrichWithDriverDetails(response, ride.getDriverId());
        }

        // Enrich with rider details
        if (ride.getRiderId() != null) {
            enrichWithRiderDetails(response, ride.getRiderId());
        }

        return response;
    }

    private void enrichWithDriverDetails(RideResponse response, Long driverId) {
        try {
            String url = "http://core-service:8081/api/drivers/" + driverId;
            @SuppressWarnings("unchecked")
            Map<String, Object> driver = restTemplate.getForObject(url, Map.class);
            if (driver != null) {
                response.setDriverName((String) driver.get("name"));
                response.setDriverPhone((String) driver.get("phone"));
                response.setDriverRating(
                        driver.get("rating") != null ? ((Number) driver.get("rating")).doubleValue() : 4.5);
                response.setVehicleType((String) driver.get("vehicleType"));
                response.setVehicleNumber((String) driver.get("vehicleNumber"));
                response.setVehicleModel((String) driver.get("vehicleModel"));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch driver details for driver {}: {}", driverId, e.getMessage());
            // Set defaults
            response.setDriverName("Driver");
            response.setDriverRating(4.5);
            response.setVehicleType("Sedan");
        }
    }

    private void enrichWithRiderDetails(RideResponse response, Long riderId) {
        try {
            String url = "http://core-service:8081/api/users/" + riderId;
            @SuppressWarnings("unchecked")
            Map<String, Object> user = restTemplate.getForObject(url, Map.class);
            if (user != null) {
                response.setRiderName((String) user.get("name"));
                response.setRiderPhone((String) user.get("phone"));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch rider details for rider {}: {}", riderId, e.getMessage());
            // Set defaults
            response.setRiderName("Rider");
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double calculatePrice(double distance) {
        // Base fare: ₹50 (minimum fare - meets Stripe minimum of 50 cents)
        double baseFare = 50.0;
        // Rate per km: ₹12 per km
        double ratePerKm = 12.0;
        // Calculate total fare
        double totalFare = baseFare + (distance * ratePerKm);
        // Round to 2 decimal places
        return Math.round(totalFare * 100.0) / 100.0;
    }
}
