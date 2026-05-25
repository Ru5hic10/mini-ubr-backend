package com.miniuber.trip.location.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for nearby drivers query
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyDriverResponse {

    /**
     * Pickup location latitude
     */
    private Double latitude;

    /**
     * Pickup location longitude
     */
    private Double longitude;

    /**
     * Search radius in kilometers
     */
    @JsonProperty("radiusKm")
    private Double radiusKm;

    /**
     * List of nearby driver IDs
     */
    private List<Long> driverIds;

    /**
     * Number of drivers found
     */
    private Integer count;
}
