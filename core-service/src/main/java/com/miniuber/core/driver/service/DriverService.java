package com.miniuber.core.driver.service;

import com.miniuber.core.driver.client.RideServiceClient;
import com.miniuber.core.driver.dto.DriverAvailabilityRequest;
import com.miniuber.core.driver.dto.DriverLocationUpdateRequest;
import com.miniuber.core.driver.dto.DriverRegistrationRequest;
import com.miniuber.core.driver.dto.DriverResponse;
import com.miniuber.core.driver.entity.Driver;
import com.miniuber.core.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverService {
    private final DriverRepository driverRepository;
    private final RideServiceClient rideServiceClient;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public DriverResponse registerDriver(DriverRegistrationRequest request) {
        if (driverRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new RuntimeException("License number already registered");
        }

        Driver driver = new Driver();
        driver.setName(request.getName());
        driver.setEmail(request.getEmail());
        driver.setPassword(passwordEncoder.encode(request.getPassword()));
        driver.setPhone(request.getPhone());
        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setVehicleType(request.getVehicleType());
        driver.setVehicleNumber(request.getVehicleNumber());
        driver.setVehicleModel(request.getVehicleModel());
        driver.setAvailable(false);
        driver.setVerified(false);

        Driver savedDriver = driverRepository.save(driver);
        return mapToResponse(savedDriver);
    }

    public DriverResponse getDriverById(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        return mapToResponse(driver);
    }

    @Transactional
    public DriverResponse updateAvailability(Long driverId, DriverAvailabilityRequest request) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        driver.setAvailable(request.getAvailable());
        // Automatically mark driver verified when they explicitly go online to avoid being filtered out
        if (Boolean.TRUE.equals(request.getAvailable())) {
            driver.setVerified(true);
        }
        Driver updatedDriver = driverRepository.save(driver);
        return mapToResponse(updatedDriver);
    }

    @Transactional
    public DriverResponse updateLocation(Long driverId, DriverLocationUpdateRequest request) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        driver.setCurrentLatitude(request.getLatitude());
        driver.setCurrentLongitude(request.getLongitude());
        
        Driver updatedDriver = driverRepository.save(driver);
        return mapToResponse(updatedDriver);
    }

    @Transactional
    public DriverResponse updateRating(Long driverId, Double newRating) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        driver.setRating(newRating);
        Driver updatedDriver = driverRepository.save(driver);
        return mapToResponse(updatedDriver);
    }

    public List<DriverResponse> getAvailableDrivers() {
        return driverRepository.findAllAvailableDrivers()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Driver getDriverByEmail(String email) {
        return driverRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
    }

    public Map<String, Object> getEarningsSummary(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        
        try {
            Double totalEarnings = rideServiceClient.getDriverTotalEarnings(driverId);
            Integer totalRides = rideServiceClient.getDriverCompletedRidesCount(driverId);
            Double averagePerRide = totalRides != null && totalRides > 0 && totalEarnings != null ? 
                totalEarnings / totalRides : 0.0;
            
            return Map.of(
                "driverId", driverId,
                "totalRides", totalRides != null ? totalRides : 0,
                "totalEarnings", totalEarnings != null ? totalEarnings : 0.0,
                "averagePerRide", averagePerRide,
                "rating", driver.getRating() != null ? driver.getRating() : 0.0
            );
        } catch (Exception e) {
            // Return zero data if ride-service is unavailable
            return Map.of(
                "driverId", driverId,
                "totalRides", 0,
                "totalEarnings", 0.0,
                "averagePerRide", 0.0,
                "rating", driver.getRating() != null ? driver.getRating() : 0.0
            );
        }
    }

    public List<Map<String, Object>> getWeeklyEarningsTrend(Long driverId) {
        List<Map<String, Object>> weeklyTrend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        try {
            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                Double dailyEarnings = rideServiceClient.getDriverDailyEarnings(driverId, date.toString());
                
                weeklyTrend.add(Map.of(
                    "date", date.toString(),
                    "earnings", dailyEarnings != null ? dailyEarnings : 0.0
                ));
            }
        } catch (Exception e) {
            // Return zero data if ride-service is unavailable
            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                weeklyTrend.add(Map.of(
                    "date", date.toString(),
                    "earnings", 0.0
                ));
            }
        }
        
        return weeklyTrend;
    }

    public Map<String, Object> getMonthlyEarnings(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        
        YearMonth currentMonth = YearMonth.now();
        
        try {
            Double monthlyEarnings = rideServiceClient.getDriverMonthlyEarnings(driverId);
            Integer monthlyRides = rideServiceClient.getDriverMonthlyRidesCount(driverId);
            Double averagePerRide = monthlyRides != null && monthlyRides > 0 && monthlyEarnings != null ? 
                monthlyEarnings / monthlyRides : 0.0;
            
            return Map.of(
                "driverId", driverId,
                "month", currentMonth.toString(),
                "totalRides", monthlyRides != null ? monthlyRides : 0,
                "totalEarnings", monthlyEarnings != null ? monthlyEarnings : 0.0,
                "averagePerRide", averagePerRide,
                "rating", driver.getRating() != null ? driver.getRating() : 0.0
            );
        } catch (Exception e) {
            // Return zero data if ride-service is unavailable
            return Map.of(
                "driverId", driverId,
                "month", currentMonth.toString(),
                "totalRides", 0,
                "totalEarnings", 0.0,
                "averagePerRide", 0.0,
                "rating", driver.getRating() != null ? driver.getRating() : 0.0
            );
        }
    }

    private DriverResponse mapToResponse(Driver driver) {
        return new DriverResponse(
                driver.getId(),
                driver.getName(),
                driver.getEmail(),
                driver.getPhone(),
                driver.getLicenseNumber(),
                driver.getVehicleType(),
                driver.getVehicleNumber(),
                driver.getVehicleModel(),
                driver.getAvailable(),
                driver.getVerified(),
                driver.getRating(),
                driver.getTotalRides(),
                driver.getCreatedAt()
        );
    }
}
