package com.miniuber.trip.ride.repository;

import com.miniuber.trip.ride.entity.RideRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for RideRating entity
 * 
 * Queries for managing ride ratings and statistics
 */
@Repository
public interface RideRatingRepository extends JpaRepository<RideRating, Long> {

    /**
     * Find all ratings for a user as the rated person
     * 
     * @param ratedUserId User ID being rated
     * @param ratingType "DRIVER" or "RIDER"
     * @return List of ratings
     */
    List<RideRating> findByRatedUserIdAndRatingType(Long ratedUserId, String ratingType);

    /**
     * Find ratings for a user ordered by most recent
     * 
     * @param ratedUserId User ID being rated
     * @param ratingType "DRIVER" or "RIDER"
     * @return List of ratings ordered by creation date descending
     */
    List<RideRating> findByRatedUserIdAndRatingTypeOrderByCreatedAtDesc(
            Long ratedUserId, String ratingType);

    /**
     * Check if a rating exists for a specific ride
     * 
     * @param rideId Ride ID
     * @param ratingType "DRIVER" or "RIDER"
     * @return True if rating exists, false otherwise
     */
    boolean existsByRideIdAndRatingType(Long rideId, String ratingType);

    /**
     * Find all ratings given by a specific user
     * 
     * @param ratingUserId User ID giving the rating
     * @return List of ratings given by the user
     */
    List<RideRating> findByRatingUserId(Long ratingUserId);

    /**
     * Count ratings for a user
     * 
     * @param ratedUserId User ID being rated
     * @param ratingType "DRIVER" or "RIDER"
     * @return Number of ratings
     */
    long countByRatedUserIdAndRatingType(Long ratedUserId, String ratingType);
}
