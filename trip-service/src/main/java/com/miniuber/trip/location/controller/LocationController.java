package com.miniuber.trip.location.controller;

import com.miniuber.trip.location.dto.LocationUpdateRequest;
import com.miniuber.trip.location.entity.DriverLocation;
import com.miniuber.trip.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LocationController {

    private final LocationService locationService;

    @PostMapping("/update")
    public ResponseEntity<DriverLocation> updateLocation(@RequestBody LocationUpdateRequest request) {
        return ResponseEntity.ok(locationService.updateLocation(request));
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<DriverLocation> getDriverLocation(@PathVariable Long driverId) {
        return ResponseEntity.ok(locationService.getDriverLocation(driverId));
    }
    
    @GetMapping("/drivers/nearby")
    public ResponseEntity<List<DriverLocation>> getNearbyDrivers(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5") Double radiusKm) {
        return ResponseEntity.ok(locationService.getNearbyDrivers(latitude, longitude, radiusKm));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Location Service is running!");
    }
}