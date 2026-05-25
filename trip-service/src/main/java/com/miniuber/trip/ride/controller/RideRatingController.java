package com.miniuber.trip.ride.controller;

import com.miniuber.trip.ride.dto.RideRatingRequest;
import com.miniuber.trip.ride.service.RideRatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Ride Rating Controller
 * REST endpoints for rating drivers and riders after ride completion
 */
@Slf4j
@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideRatingController {

    private final RideRatingService rideRatingService;

    /**
     * PUT /api/rides/{rideId}/rate-driver
     * Rate the driver after ride completion
     * 
     * Request body:
     * {
     *   "rating": 5,
     *   "comment": "Great driver, very friendly!"
     * }
     * 
     * Rating must be between 1-5 stars
     * 
     * @param rideId Ride ID
     * @param request Rating request with score and optional comment
     * @return 200 OK with updated ride and driver rating
     */
    @PutMapping("/{rideId}/rate-driver")
    public ResponseEntity<Map<String, Object>> rateDriver(
            @PathVariable Long rideId,
            @RequestBody RideRatingRequest request) {

        log.info("Rating driver for ride {}: rating={}, comment='{}'",
                rideId, request.getRating(), request.getComment());

        // Validate rating is between 1-5
        if (request.getRating() < 1 || request.getRating() > 5) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Rating must be between 1 and 5 stars"));
        }

        try {
            Map<String, Object> result = rideRatingService.submitDriverRating(
                    rideId,
                    request.getRating(),
                    request.getComment()
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error rating driver for ride {}", rideId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/rides/{rideId}/rate-rider
     * Rate the rider after ride completion (by driver)
     * 
     * @param rideId Ride ID
     * @param request Rating request with score and optional comment
     * @return 200 OK
     */
    @PutMapping("/{rideId}/rate-rider")
    public ResponseEntity<Map<String, Object>> rateRider(
            @PathVariable Long rideId,
            @RequestBody RideRatingRequest request) {

        log.info("Rating rider for ride {}: rating={}", rideId, request.getRating());

        if (request.getRating() < 1 || request.getRating() > 5) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Rating must be between 1 and 5 stars"));
        }

        try {
            Map<String, Object> result = rideRatingService.submitRiderRating(
                    rideId,
                    request.getRating(),
                    request.getComment()
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error rating rider for ride {}", rideId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/drivers/{driverId}/ratings
     * Get average rating for a driver
     * 
     * Response:
     * {
     *   "driverId": 123,
     *   "averageRating": 4.5,
     *   "totalRatings": 50,
     *   "ratingDistribution": {
     *     "5": 30,
     *     "4": 15,
     *     "3": 3,
     *     "2": 2,
     *     "1": 0
     *   }
     * }
     * 
     * @param driverId Driver ID
     * @return Driver's average rating and rating distribution
     */
    @GetMapping("/driver/{driverId}/ratings")
    public ResponseEntity<Map<String, Object>> getDriverRating(@PathVariable Long driverId) {
        log.info("Fetching rating for driver {}", driverId);

        try {
            Map<String, Object> rating = rideRatingService.getDriverRatingStats(driverId);
            return ResponseEntity.ok(rating);
        } catch (Exception e) {
            log.error("Error fetching driver rating", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/riders/{riderId}/ratings
     * Get average rating for a rider
     * 
     * @param riderId Rider ID
     * @return Rider's average rating
     */
    @GetMapping("/rider/{riderId}/ratings")
    public ResponseEntity<Map<String, Object>> getRiderRating(@PathVariable Long riderId) {
        log.info("Fetching rating for rider {}", riderId);

        try {
            Map<String, Object> rating = rideRatingService.getRiderRatingStats(riderId);
            return ResponseEntity.ok(rating);
        } catch (Exception e) {
            log.error("Error fetching rider rating", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
