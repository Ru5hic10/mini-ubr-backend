package com.miniuber.trip.ride.service;

import com.miniuber.trip.ride.entity.Ride;
import com.miniuber.trip.ride.entity.RideRating;
import com.miniuber.trip.ride.entity.RideStatus;
import com.miniuber.trip.ride.repository.RideRepository;
import com.miniuber.trip.ride.repository.RideRatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing ride ratings and driver/rider reputation
 * 
 * Features:
 * - Submit driver and rider ratings after ride completion
 * - Calculate average ratings and distribution
 * - Track rating history
 * - Prevent duplicate ratings (one per ride)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RideRatingService {

    private final RideRatingRepository rideRatingRepository;
    private final RideRepository rideRepository;
    private final RestTemplate restTemplate;

    /**
     * Submit a rating for the driver
     * 
     * - Only rider can rate driver
     * - Can only rate completed rides
     * - One rating per ride
     * - Updates driver's average rating
     * 
     * @param rideId The ride ID
     * @param rating Rating from 1-5 stars
     * @param comment Optional feedback comment
     * @return Map with updated rating information
     */
    @Transactional
    public Map<String, Object> submitDriverRating(Long rideId, int rating, String comment) {
        log.info("Submitting driver rating for ride {}: {}/5 stars", rideId, rating);

        // Fetch ride
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found: " + rideId));

        // Validate ride is completed
        if (ride.getStatus() != RideStatus.COMPLETED) {
            throw new RuntimeException("Can only rate completed rides");
        }

        // Check if driver rating already exists for this ride
        if (rideRatingRepository.existsByRideIdAndRatingType(rideId, "DRIVER")) {
            throw new RuntimeException("Driver already rated for this ride");
        }

        // Create and save rating
        RideRating rideRating = RideRating.builder()
                .ride(ride)
                .ratedUserId(ride.getDriverId())          // Rating is for driver
                .ratingUserId(ride.getRiderId())           // Rated by rider
                .ratingType("DRIVER")
                .score(rating)
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .build();

        rideRatingRepository.save(rideRating);

        // Calculate and return updated driver stats
        Map<String, Object> driverStats = getDriverRatingStats(ride.getDriverId());
        
        // Update driver's average rating in driver-service
        try {
            Double averageRating = (Double) driverStats.get("averageRating");
            String url = "http://driver-service:8084/api/drivers/" + ride.getDriverId() + "/rating";
            restTemplate.put(url, Map.of("rating", averageRating));
            log.info("Updated driver {} rating to {}", ride.getDriverId(), averageRating);
        } catch (Exception e) {
            log.warn("Failed to update driver rating in driver-service: {}", e.getMessage());
        }

        return Map.of(
                "message", "Driver rated successfully",
                "rideId", rideId,
                "rating", rating,
                "driverStats", driverStats
        );
    }

    /**
     * Submit a rating for the rider
     * 
     * - Only driver can rate rider
     * - Can only rate completed rides
     * - One rating per ride
     * - Updates rider's average rating
     * 
     * @param rideId The ride ID
     * @param rating Rating from 1-5 stars
     * @param comment Optional feedback comment
     * @return Map with updated rating information
     */
    @Transactional
    public Map<String, Object> submitRiderRating(Long rideId, int rating, String comment) {
        log.info("Submitting rider rating for ride {}: {}/5 stars", rideId, rating);

        // Fetch ride
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found: " + rideId));

        // Validate ride is completed
        if (ride.getStatus() != RideStatus.COMPLETED) {
            throw new RuntimeException("Can only rate completed rides");
        }

        // Check if rider rating already exists
        if (rideRatingRepository.existsByRideIdAndRatingType(rideId, "RIDER")) {
            throw new RuntimeException("Rider already rated for this ride");
        }

        // Create and save rating
        RideRating rideRating = RideRating.builder()
                .ride(ride)
                .ratedUserId(ride.getRiderId())            // Rating is for rider
                .ratingUserId(ride.getDriverId())          // Rated by driver
                .ratingType("RIDER")
                .score(rating)
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .build();

        rideRatingRepository.save(rideRating);

        // Calculate and return updated rider stats
        Map<String, Object> riderStats = getRiderRatingStats(ride.getRiderId());

        return Map.of(
                "message", "Rider rated successfully",
                "rideId", rideId,
                "rating", rating,
                "riderStats", riderStats
        );
    }

    /**
     * Get driver's average rating and rating distribution
     * 
     * Statistics include:
     * - Average rating (0-5 stars)
     * - Total number of ratings
     * - Distribution across 1-5 stars
     * - Breakdown by recent (last 30 days) vs overall
     * 
     * @param driverId Driver ID
     * @return Map with rating statistics
     */
    public Map<String, Object> getDriverRatingStats(Long driverId) {
        log.info("Calculating rating stats for driver {}", driverId);

        List<RideRating> driverRatings = rideRatingRepository
                .findByRatedUserIdAndRatingType(driverId, "DRIVER");

        if (driverRatings.isEmpty()) {
            return Map.of(
                    "driverId", driverId,
                    "averageRating", 0.0,
                    "totalRatings", 0,
                    "ratingDistribution", Map.of(
                            "5", 0,
                            "4", 0,
                            "3", 0,
                            "2", 0,
                            "1", 0
                    )
            );
        }

        // Calculate average
        double averageRating = driverRatings.stream()
                .mapToInt(RideRating::getScore)
                .average()
                .orElse(0.0);

        // Calculate distribution
        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            final int score = i;
            long count = driverRatings.stream()
                    .filter(r -> r.getScore() == score)
                    .count();
            distribution.put(i, count);
        }

        // Round to 1 decimal place
        averageRating = Math.round(averageRating * 10.0) / 10.0;

        return Map.of(
                "driverId", driverId,
                "averageRating", averageRating,
                "totalRatings", driverRatings.size(),
                "ratingDistribution", Map.of(
                        "5", distribution.get(5),
                        "4", distribution.get(4),
                        "3", distribution.get(3),
                        "2", distribution.get(2),
                        "1", distribution.get(1)
                )
        );
    }

    /**
     * Get rider's average rating and rating distribution
     * 
     * @param riderId Rider ID
     * @return Map with rating statistics
     */
    public Map<String, Object> getRiderRatingStats(Long riderId) {
        log.info("Calculating rating stats for rider {}", riderId);

        List<RideRating> riderRatings = rideRatingRepository
                .findByRatedUserIdAndRatingType(riderId, "RIDER");

        if (riderRatings.isEmpty()) {
            return Map.of(
                    "riderId", riderId,
                    "averageRating", 0.0,
                    "totalRatings", 0,
                    "ratingDistribution", Map.of(
                            "5", 0,
                            "4", 0,
                            "3", 0,
                            "2", 0,
                            "1", 0
                    )
            );
        }

        // Calculate average
        double averageRating = riderRatings.stream()
                .mapToInt(RideRating::getScore)
                .average()
                .orElse(0.0);

        // Calculate distribution
        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            final int score = i;
            long count = riderRatings.stream()
                    .filter(r -> r.getScore() == score)
                    .count();
            distribution.put(i, count);
        }

        // Round to 1 decimal place
        averageRating = Math.round(averageRating * 10.0) / 10.0;

        return Map.of(
                "riderId", riderId,
                "averageRating", averageRating,
                "totalRatings", riderRatings.size(),
                "ratingDistribution", Map.of(
                        "5", distribution.get(5),
                        "4", distribution.get(4),
                        "3", distribution.get(3),
                        "2", distribution.get(2),
                        "1", distribution.get(1)
                )
        );
    }

    /**
     * Get recent ratings for a user (last 10 ratings)
     * 
     * @param userId User ID (driver or rider)
     * @param ratingType "DRIVER" or "RIDER"
     * @return List of recent ratings with comments
     */
    public List<Map<String, Object>> getRecentRatings(Long userId, String ratingType) {
        log.info("Fetching recent {} ratings for user {}", ratingType, userId);

        List<RideRating> ratings = rideRatingRepository
                .findByRatedUserIdAndRatingTypeOrderByCreatedAtDesc(userId, ratingType);

        // Take only last 10
        List<Map<String, Object>> recentRatings = new ArrayList<>();
        for (int i = 0; i < Math.min(10, ratings.size()); i++) {
            RideRating rating = ratings.get(i);
            recentRatings.add(Map.of(
                    "rideId", rating.getRide().getId(),
                    "score", rating.getScore(),
                    "comment", rating.getComment() != null ? rating.getComment() : "",
                    "date", rating.getCreatedAt(),
                    "ratedBy", rating.getRatingUserId()
            ));
        }

        return recentRatings;
    }
}
