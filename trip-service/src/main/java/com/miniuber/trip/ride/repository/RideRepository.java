package com.miniuber.trip.ride.repository;

import com.miniuber.trip.ride.entity.Ride;
import com.miniuber.trip.ride.entity.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByRiderId(Long riderId);
    List<Ride> findByDriverId(Long driverId);
    List<Ride> findByStatus(RideStatus status);
    List<Ride> findByDriverIdAndStatus(Long driverId, String status);
}