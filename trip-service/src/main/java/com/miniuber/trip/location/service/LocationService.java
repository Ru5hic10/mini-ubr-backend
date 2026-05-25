package com.miniuber.trip.location.service;

import com.miniuber.trip.location.dto.LocationUpdateRequest;
import com.miniuber.trip.location.entity.DriverLocation;
import com.miniuber.trip.location.repository.DriverLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final DriverLocationRepository repository;

    @Transactional
    public DriverLocation updateLocation(LocationUpdateRequest request) {
        DriverLocation location = repository.findById(request.getDriverId())
                .orElse(new DriverLocation());
        
        location.setDriverId(request.getDriverId());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        
        return repository.save(location);
    }

    public DriverLocation getDriverLocation(Long driverId) {
        return repository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Location not found for driver"));
    }
    
    public List<DriverLocation> getNearbyDrivers(Double latitude, Double longitude, Double radiusKm) {
        // Convert radius to approximate lat/lon offset
        // 1 degree latitude ≈ 111 km
        // 1 degree longitude ≈ 111 * cos(latitude) km
        double latOffset = radiusKm / 111.0;
        double lonOffset = radiusKm / (111.0 * Math.cos(Math.toRadians(latitude)));
        
        double minLat = latitude - latOffset;
        double maxLat = latitude + latOffset;
        double minLon = longitude - lonOffset;
        double maxLon = longitude + lonOffset;
        
        return repository.findNearbyDrivers(minLat, maxLat, minLon, maxLon);
    }
}