package com.miniuber.trip.ride.service;

import com.miniuber.trip.ride.algorithm.RideMatchingAlgorithm;
import com.miniuber.trip.ride.dto.DriverScore;
import com.miniuber.trip.ride.repository.DriverLocationRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Ride Matching Service
 * Orchestrates the ride matching process using algorithm and data repositories
 * 
 * Process:
 * 1. Query Redis Geo for drivers within 5km of pickup location
 * 2. Fetch driver details from database (rating, acceptance rate, etc.)
 * 3. Score drivers using RideMatchingAlgorithm
 * 4. Select top 5 drivers for sending ride offers
 * 5. Send Kafka event to driver service for WebSocket notifications
 * 6. Log matching results for debugging
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RideMatchingService {

    private final RideMatchingAlgorithm matchingAlgorithm;
    private final DriverLocationRedisRepository driverLocationRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Configuration
    private static final double INITIAL_SEARCH_RADIUS_KM = 5.0;
    private static final double MAX_SEARCH_RADIUS_KM = 20.0;
    private static final int TOP_DRIVERS_TO_OFFER = 5;
    private static final long OFFER_TIMEOUT_SECONDS = 30;

    /**
     * Find best matching drivers for a ride request
     * 
     * @param rideId The ride ID
     * @param pickupLatitude Pickup location latitude
     * @param pickupLongitude Pickup location longitude
     * @param dropoffLatitude Dropoff location latitude
     * @param dropoffLongitude Dropoff location longitude
     * @param estimatedFare Estimated fare amount
     * @param driverRepository Repository to fetch driver details
     * @return List of top matching drivers
     */
    public List<DriverScore> findMatchingDrivers(Long rideId,
                                                 Double pickupLatitude,
                                                 Double pickupLongitude,
                                                 Double dropoffLatitude,
                                                 Double dropoffLongitude,
                                                 Double estimatedFare,
                                                 DriverRepository driverRepository) {

        log.info("Finding matching drivers for ride {}: pickup ({}, {}), dropoff ({}, {})",
                rideId, pickupLatitude, pickupLongitude, dropoffLatitude, dropoffLongitude);

        // Start with initial search radius and expand if needed
        List<DriverScore> topDrivers = findDriversWithRadius(
                pickupLatitude, pickupLongitude, 
                INITIAL_SEARCH_RADIUS_KM, 
                driverRepository
        );

        // If not enough drivers found, expand search radius
        if (topDrivers.size() < TOP_DRIVERS_TO_OFFER) {
            topDrivers = findDriversWithRadius(
                    pickupLatitude, pickupLongitude,
                    MAX_SEARCH_RADIUS_KM,
                    driverRepository
            );
            log.warn("Expanded search radius to {}km for ride {}, found {} drivers",
                    MAX_SEARCH_RADIUS_KM, rideId, topDrivers.size());
        }

        log.info("Found {} matching drivers for ride {} (top {} will be offered)",
                topDrivers.size(), rideId, TOP_DRIVERS_TO_OFFER);

        return topDrivers.stream()
                .limit(TOP_DRIVERS_TO_OFFER)
                .collect(Collectors.toList());
    }

    /**
     * Find drivers within a specific radius and return top matches
     */
    private List<DriverScore> findDriversWithRadius(Double pickupLatitude,
                                                    Double pickupLongitude,
                                                    Double radiusKm,
                                                    DriverRepository driverRepository) {

        // Step 1: Get all drivers within radius from Redis Geo index
        List<Long> nearbyDriverIds = driverLocationRepository.findDriversWithinRadius(
                pickupLatitude,
                pickupLongitude,
                radiusKm
        );

        if (nearbyDriverIds.isEmpty()) {
            log.warn("No drivers found within {}km of ({}, {})",
                    radiusKm, pickupLatitude, pickupLongitude);
            return List.of();
        }

        // Step 2: Fetch driver details from database
        List<Driver> drivers = driverRepository.findByIdIn(nearbyDriverIds);

        if (drivers.isEmpty()) {
            log.warn("No driver details found for {} nearby driver IDs", nearbyDriverIds.size());
            return List.of();
        }

        // Step 3: Convert to DriverInfo and calculate scores
        List<RideMatchingAlgorithm.DriverInfo> driverInfoList = drivers.stream()
                .map(d -> new RideMatchingAlgorithm.DriverInfo(
                        d.getId(),
                        d.getCurrentLatitude(),
                        d.getCurrentLongitude(),
                        d.getRating(),
                        calculateAcceptanceRate(d),
                        d.getTotalRides()
                ))
                .collect(Collectors.toList());

        // Step 4: Score all drivers and return top matches
        return matchingAlgorithm.findTopDrivers(
                driverInfoList,
                pickupLatitude,
                pickupLongitude,
                TOP_DRIVERS_TO_OFFER
        );
    }

    /**
     * Publish ride offer to selected drivers via Kafka
     * Driver service will consume and send WebSocket notifications
     * 
     * @param rideId Ride ID
     * @param driverId Driver ID
     * @param riderId Rider ID
     * @param pickupLocation Pickup location string
     * @param dropoffLocation Dropoff location string
     * @param estimatedFare Estimated fare
     * @param distance Distance in km
     */
    public void publishRideOfferToDriver(Long rideId,
                                        Long driverId,
                                        Long riderId,
                                        String pickupLocation,
                                        String dropoffLocation,
                                        Double estimatedFare,
                                        Double distance) {

        try {
            String message = String.format(
                    "{\"rideId\":%d,\"driverId\":%d,\"riderId\":%d," +
                    "\"pickupLocation\":\"%s\",\"dropoffLocation\":\"%s\"," +
                    "\"estimatedFare\":%.2f,\"distance\":%.2f,\"offerExpiresAt\":%d}",
                    rideId, driverId, riderId,
                    pickupLocation, dropoffLocation,
                    estimatedFare, distance,
                    System.currentTimeMillis() + (OFFER_TIMEOUT_SECONDS * 1000)
            );

            kafkaTemplate.send("drivers.notification", message);

            log.info("Published ride offer for ride {} to driver {} (fare: ₹{}, distance: {}km)",
                    rideId, driverId, estimatedFare, distance);
        } catch (Exception e) {
            log.error("Error publishing ride offer for ride {}", rideId, e);
        }
    }

    /**
     * Calculate acceptance rate for a driver
     * If driver has never accepted any ride, return 0.5 (50% default)
     */
    private Double calculateAcceptanceRate(Driver driver) {
        if (driver.getTotalRides() == null || driver.getTotalRides() == 0) {
            return 0.5;  // Default acceptance rate for new drivers
        }
        // This would need to be stored/calculated in database
        // For now, return 0.8 as placeholder (80% acceptance rate)
        return 0.8;
    }

    /**
     * Placeholder for Driver entity (you'll need to implement this based on your model)
     */
    public interface DriverRepository {
        List<Driver> findByIdIn(List<Long> ids);
    }

    public interface Driver {
        Long getId();
        Double getCurrentLatitude();
        Double getCurrentLongitude();
        Double getRating();
        Integer getTotalRides();
    }
}
