package com.miniuber.trip.ride.algorithm;

import com.miniuber.trip.ride.dto.DriverScore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ride Matching Algorithm using Strategy Pattern
 * 
 * Scoring Logic:
 * - Distance (40% weight): Closer drivers ranked higher
 * - Rating (30% weight): Higher rated drivers ranked higher
 * - Acceptance Rate (30% weight): Drivers with better acceptance history ranked higher
 * 
 * Final Score = (distance_score * 0.4) + (rating_score * 0.3) + (acceptance_score * 0.3)
 * 
 * All scores normalized to 0-100 range for fair comparison
 */
@Slf4j
@Component
public class RideMatchingAlgorithm {

    // Weighting constants
    private static final double DISTANCE_WEIGHT = 0.40;
    private static final double RATING_WEIGHT = 0.30;
    private static final double ACCEPTANCE_WEIGHT = 0.30;

    // Maximum distance threshold (in km)
    private static final double MAX_DISTANCE_KM = 50.0;

    // Minimum acceptable ratings (filters)
    private static final double MIN_ACCEPTABLE_RATING = 3.5;
    private static final double MIN_ACCEPTABLE_ACCEPTANCE_RATE = 0.70;  // 70%

    /**
     * Calculate matching score for a driver based on multiple criteria
     * 
     * @param driver Driver information with location and stats
     * @param pickupLatitude Rider's pickup latitude
     * @param pickupLongitude Rider's pickup longitude
     * @return DriverScore with calculated score and components
     */
    public DriverScore calculateScore(DriverInfo driver, 
                                     Double pickupLatitude, 
                                     Double pickupLongitude) {

        // Calculate distance from driver to pickup location
        double distance = calculateHaversineDistance(
                driver.getLatitude(),
                driver.getLongitude(),
                pickupLatitude,
                pickupLongitude
        );

        // Filter out drivers beyond max distance
        if (distance > MAX_DISTANCE_KM) {
            return DriverScore.builder()
                    .driverId(driver.getId())
                    .score(0.0)
                    .distance(distance)
                    .rating(driver.getRating())
                    .acceptanceRate(driver.getAcceptanceRate())
                    .reason("Too far away (>" + MAX_DISTANCE_KM + "km)")
                    .build();
        }

        // Calculate individual normalized scores (0-100)
        double distanceScore = calculateDistanceScore(distance);
        double ratingScore = calculateRatingScore(driver.getRating());
        double acceptanceScore = calculateAcceptanceRateScore(driver.getAcceptanceRate());

        // Apply minimum threshold filters
        if (driver.getRating() < MIN_ACCEPTABLE_RATING) {
            return DriverScore.builder()
                    .driverId(driver.getId())
                    .score(0.0)
                    .distance(distance)
                    .rating(driver.getRating())
                    .acceptanceRate(driver.getAcceptanceRate())
                    .reason("Rating below minimum threshold")
                    .build();
        }

        if (driver.getAcceptanceRate() < MIN_ACCEPTABLE_ACCEPTANCE_RATE) {
            return DriverScore.builder()
                    .driverId(driver.getId())
                    .score(0.0)
                    .distance(distance)
                    .rating(driver.getRating())
                    .acceptanceRate(driver.getAcceptanceRate())
                    .reason("Acceptance rate below minimum threshold")
                    .build();
        }

        // Calculate weighted final score
        double finalScore = (distanceScore * DISTANCE_WEIGHT) 
                          + (ratingScore * RATING_WEIGHT) 
                          + (acceptanceScore * ACCEPTANCE_WEIGHT);

        return DriverScore.builder()
                .driverId(driver.getId())
                .score(finalScore)
                .distance(distance)
                .rating(driver.getRating())
                .acceptanceRate(driver.getAcceptanceRate())
                .distanceScore(distanceScore)
                .ratingScore(ratingScore)
                .acceptanceScore(acceptanceScore)
                .build();
    }

    /**
     * Find top N matching drivers and return sorted by score (descending)
     * 
     * @param drivers List of available drivers
     * @param pickupLatitude Rider's pickup latitude
     * @param pickupLongitude Rider's pickup longitude
     * @param topN Number of top drivers to return
     * @return List of top N drivers sorted by matching score
     */
    public List<DriverScore> findTopDrivers(List<DriverInfo> drivers,
                                           Double pickupLatitude,
                                           Double pickupLongitude,
                                           int topN) {

        log.info("Finding top {} drivers for pickup location ({}, {})", 
                topN, pickupLatitude, pickupLongitude);

        List<DriverScore> scores = new ArrayList<>();

        // Calculate scores for all drivers
        for (DriverInfo driver : drivers) {
            DriverScore score = calculateScore(driver, pickupLatitude, pickupLongitude);
            scores.add(score);
        }

        // Sort by score descending and take top N
        return scores.stream()
                .filter(s -> s.getScore() > 0.0)  // Filter out invalid scores
                .sorted(Comparator.comparingDouble(DriverScore::getScore).reversed())
                .limit(topN)
                .toList();
    }

    /**
     * Calculate distance score (0-100)
     * Closer distance = higher score
     * Normalized: 0km = 100, MAX_DISTANCE_KM = 0
     */
    private double calculateDistanceScore(double distanceKm) {
        if (distanceKm <= 0) return 100.0;
        if (distanceKm >= MAX_DISTANCE_KM) return 0.0;

        // Linear scoring: score decreases as distance increases
        return 100.0 * (1.0 - (distanceKm / MAX_DISTANCE_KM));
    }

    /**
     * Calculate rating score (0-100)
     * Higher rating = higher score
     * Normalized: 5.0 = 100, 0.0 = 0
     */
    private double calculateRatingScore(Double rating) {
        if (rating == null || rating <= 0) return 0.0;
        if (rating >= 5.0) return 100.0;

        // Linear scoring: (rating / 5.0) * 100
        return (rating / 5.0) * 100.0;
    }

    /**
     * Calculate acceptance rate score (0-100)
     * Higher acceptance rate = higher score
     * Normalized: 1.0 (100%) = 100, 0.0 = 0
     */
    private double calculateAcceptanceRateScore(Double acceptanceRate) {
        if (acceptanceRate == null || acceptanceRate <= 0) return 0.0;
        if (acceptanceRate >= 1.0) return 100.0;

        // Linear scoring: (rate * 100)
        return acceptanceRate * 100.0;
    }

    /**
     * Haversine formula to calculate distance between two geographic coordinates
     * Returns distance in kilometers
     * 
     * @param lat1 Source latitude
     * @param lon1 Source longitude
     * @param lat2 Destination latitude
     * @param lon2 Destination longitude
     * @return Distance in kilometers
     */
    private double calculateHaversineDistance(Double lat1, Double lon1,
                                             Double lat2, Double lon2) {

        final double EARTH_RADIUS_KM = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Inner class for driver information used in matching
     */
    public static class DriverInfo {
        private Long id;
        private Double latitude;
        private Double longitude;
        private Double rating;
        private Double acceptanceRate;
        private Integer totalRides;

        // Constructors
        public DriverInfo(Long id, Double latitude, Double longitude, 
                         Double rating, Double acceptanceRate, Integer totalRides) {
            this.id = id;
            this.latitude = latitude;
            this.longitude = longitude;
            this.rating = rating != null ? rating : 0.0;
            this.acceptanceRate = acceptanceRate != null ? acceptanceRate : 0.0;
            this.totalRides = totalRides != null ? totalRides : 0;
        }

        // Getters
        public Long getId() { return id; }
        public Double getLatitude() { return latitude; }
        public Double getLongitude() { return longitude; }
        public Double getRating() { return rating; }
        public Double getAcceptanceRate() { return acceptanceRate; }
        public Integer getTotalRides() { return totalRides; }
    }
}
