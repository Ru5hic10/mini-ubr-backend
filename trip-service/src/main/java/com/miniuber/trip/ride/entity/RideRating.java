package com.miniuber.trip.ride.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * RideRating Entity
 * 
 * Stores ratings given by users (driver or rider) after ride completion
 * 
 * Fields:
 * - ratedUserId: The user being rated (driver or rider)
 * - ratingUserId: The user giving the rating
 * - ratingType: "DRIVER" or "RIDER"
 * - score: Rating from 1-5 stars
 * - comment: Optional feedback text
 */
@Entity
@Table(name = "ride_ratings", indexes = {
        @Index(name = "idx_rated_user_type", columnList = "rated_user_id,rating_type"),
        @Index(name = "idx_ride_rating_type", columnList = "ride_id,rating_type"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    /**
     * User ID of the person being rated
     */
    @Column(nullable = false)
    private Long ratedUserId;

    /**
     * User ID of the person giving the rating
     */
    @Column(nullable = false)
    private Long ratingUserId;

    /**
     * Type of rating: "DRIVER" or "RIDER"
     */
    @Column(nullable = false, length = 20)
    private String ratingType;

    /**
     * Rating score: 1-5 stars
     */
    @Column(nullable = false)
    private Integer score;

    /**
     * Optional feedback comment
     */
    @Column(length = 500)
    private String comment;

    /**
     * When the rating was created
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
