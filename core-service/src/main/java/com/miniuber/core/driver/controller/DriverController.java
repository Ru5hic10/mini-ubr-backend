package com.miniuber.core.driver.controller;

import com.miniuber.core.driver.dto.DriverAvailabilityRequest;
import com.miniuber.core.driver.dto.DriverLocationUpdateRequest;
import com.miniuber.core.driver.dto.DriverRegistrationRequest;
import com.miniuber.core.driver.dto.DriverResponse;
import com.miniuber.core.driver.entity.Driver;
import com.miniuber.core.driver.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DriverController {
    private final DriverService driverService;

    @PostMapping("/register")
    public ResponseEntity<DriverResponse> registerDriver(
            @Valid @RequestBody DriverRegistrationRequest request) {
        DriverResponse response = driverService.registerDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Alias to support POST /api/drivers with minimal fields
    @PostMapping("")
    public ResponseEntity<DriverResponse> registerDriverAlias(@RequestBody java.util.Map<String, String> body) {
        DriverRegistrationRequest req = new DriverRegistrationRequest();
        req.setName(body.getOrDefault("name", "Driver"));
        req.setEmail(body.getOrDefault("email", "driver@example.com"));
        req.setLicenseNumber(body.getOrDefault("licenseNumber", "DLTEMP123"));
        // Provide sensible defaults for required fields
        req.setPassword("password123");
        req.setPhone("0000000000");
        req.setVehicleType("Sedan");
        req.setVehicleNumber("TEMP123");
        req.setVehicleModel("Standard");
        DriverResponse response = driverService.registerDriver(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getDriver(@PathVariable Long id) {
        DriverResponse response = driverService.getDriverById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/availability")
    public ResponseEntity<DriverResponse> updateAvailability(
            @PathVariable Long id,
            @RequestBody DriverAvailabilityRequest request) {
        DriverResponse response = driverService.updateAvailability(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/location")
    public ResponseEntity<DriverResponse> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody DriverLocationUpdateRequest request) {
        DriverResponse response = driverService.updateLocation(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/rating")
    public ResponseEntity<DriverResponse> updateRating(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Double> request) {
        Double newRating = request.get("rating");
        if (newRating == null || newRating < 0 || newRating > 5) {
            return ResponseEntity.badRequest().build();
        }
        DriverResponse response = driverService.updateRating(id, newRating);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/available")
    public ResponseEntity<List<DriverResponse>> getAvailableDrivers() {
        List<DriverResponse> drivers = driverService.getAvailableDrivers();
        return ResponseEntity.ok(drivers);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Driver Service is running!");
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<Driver> getDriverByEmail(@PathVariable String email) {
        Driver driver = driverService.getDriverByEmail(email);
        return ResponseEntity.ok(driver);
    }
}
