package com.miniuber.trip.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a driver's matching score
 * Contains calculated score and all scoring components for transparency
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverScore {

    /**
     * Driver ID being scored
     */
    private Long driverId;

    /**
     * Final weighted score (0-100)
     * Used for ranking and selecting top drivers
     */
    private Double score;

    /**
     * Distance from driver to pickup location (in km)
     */
    private Double distance;

    /**
     * Driver's average rating (1-5 stars)
     */
    private Double rating;

    /**
     * Driver's ride acceptance rate (0-1, where 1 = 100%)
     */
    private Double acceptanceRate;

    /**
     * Normalized distance score component (0-100)
     * Closer distance = higher score
     */
    private Double distanceScore;

    /**
     * Normalized rating score component (0-100)
     * Higher rating = higher score
     */
    private Double ratingScore;

    /**
     * Normalized acceptance rate score component (0-100)
     * Better acceptance history = higher score
     */
    private Double acceptanceScore;

    /**
     * Reason if driver is not qualified (e.g., "Too far away")
     */
    private String reason;
}
