package com.miniuber.trip.location.repository;

import com.miniuber.trip.location.entity.DriverLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DriverLocationRepository extends JpaRepository<DriverLocation, Long> {
    
    // Find all drivers within a bounding box (simple approach)
    @Query("SELECT d FROM DriverLocation d WHERE " +
           "d.latitude BETWEEN :minLat AND :maxLat AND " +
           "d.longitude BETWEEN :minLon AND :maxLon")
    List<DriverLocation> findNearbyDrivers(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon
    );
}