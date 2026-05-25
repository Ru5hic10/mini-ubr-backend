package com.miniuber.trip.ride.controller;

import com.miniuber.trip.ride.dto.RideRequest;
import com.miniuber.trip.ride.dto.RideResponse;
import com.miniuber.trip.ride.service.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RideController {

    private final RideService rideService;

    @PostMapping("/request")
    public ResponseEntity<RideResponse> requestRide(@RequestBody RideRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rideService.requestRide(request));
    }

    // Alias to support POST /api/rides as used by test script
    @PostMapping("")
    public ResponseEntity<RideResponse> createRideAlias(@RequestBody RideRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rideService.requestRide(request));
    }

    @PutMapping("/{id}/accept/{driverId}")
    public ResponseEntity<RideResponse> acceptRide(@PathVariable Long id, @PathVariable Long driverId) {
        return ResponseEntity.ok(rideService.acceptRide(id, driverId));
    }

    // Alias to support PUT /api/rides/{id}/accept without driverId
    @PutMapping("/{id}/accept")
    public ResponseEntity<RideResponse> acceptRideAlias(@PathVariable Long id) {
        Long defaultDriverId = 1L;
        return ResponseEntity.ok(rideService.acceptRide(id, defaultDriverId));
    }

    @PutMapping("/{id}/start")
    public ResponseEntity<RideResponse> startRide(@PathVariable Long id) {
        return ResponseEntity.ok(rideService.startRide(id));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<RideResponse> completeRide(@PathVariable Long id) {
        return ResponseEntity.ok(rideService.completeRide(id));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<RideResponse> cancelRide(@PathVariable Long id) {
        return ResponseEntity.ok(rideService.cancelRide(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RideResponse> getRide(@PathVariable Long id) {
        return ResponseEntity.ok(rideService.getRideById(id));
    }

    @GetMapping("/rider/{riderId}")
    public ResponseEntity<List<RideResponse>> getRidesByRider(@PathVariable Long riderId) {
        return ResponseEntity.ok(rideService.getRidesByRider(riderId));
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<RideResponse>> getRidesByDriver(@PathVariable Long driverId) {
        return ResponseEntity.ok(rideService.getRidesByDriver(driverId));
    }
    
    @GetMapping("/available")
    public ResponseEntity<List<RideResponse>> getAvailableRides() {
        return ResponseEntity.ok(rideService.getAvailableRides());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Ride Service is running!");
    }

        // Update payment status for a ride
        @PutMapping("/{id}/payment-status")
        public ResponseEntity<RideResponse> updatePaymentStatus(@PathVariable Long id, @RequestParam String status) {
            return ResponseEntity.ok(rideService.updatePaymentStatus(id, status));
        }
}