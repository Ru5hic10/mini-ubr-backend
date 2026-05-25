package com.miniuber.trip.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RideRatingRequest DTO
 * 
 * Request body for submitting ratings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideRatingRequest {

    /**
     * Rating score (1-5 stars)
     */
    private Integer rating;

    /**
     * Optional feedback comment
     */
    private String comment;
}
