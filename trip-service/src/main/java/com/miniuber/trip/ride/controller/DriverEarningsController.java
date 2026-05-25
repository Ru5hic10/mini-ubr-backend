package com.miniuber.trip.ride.controller;

import com.miniuber.trip.ride.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Controller for driver earnings endpoints
 * Called by driver-service via Feign client
 */
@RestController
@RequestMapping("/api/rides/driver")
@RequiredArgsConstructor
@Slf4j
public class DriverEarningsController {
    
    private final RideRepository rideRepository;
    
    /**
     * Get total earnings for a driver
     */
    @GetMapping("/{driverId}/earnings/total")
    public ResponseEntity<Double> getDriverTotalEarnings(@PathVariable Long driverId) {
        log.info("Fetching total earnings for driver: {}", driverId);
        
        Double totalEarnings = rideRepository.findByDriverIdAndStatus(driverId, "COMPLETED")
                .stream()
                .mapToDouble(ride -> ride.getPrice() != null ? ride.getPrice() : 0.0)
                .sum();
        
        return ResponseEntity.ok(totalEarnings);
    }
    
    /**
     * Get completed rides count for a driver
     */
    @GetMapping("/{driverId}/completed-count")
    public ResponseEntity<Integer> getDriverCompletedRidesCount(@PathVariable Long driverId) {
        log.info("Fetching completed rides count for driver: {}", driverId);
        
        int count = rideRepository.findByDriverIdAndStatus(driverId, "COMPLETED").size();
        
        return ResponseEntity.ok(count);
    }
    
    /**
     * Get daily earnings for a driver
     */
    @GetMapping("/{driverId}/earnings/daily")
    public ResponseEntity<Double> getDriverDailyEarnings(
            @PathVariable Long driverId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String date) {
        
        log.info("Fetching daily earnings for driver: {} on date: {}", driverId, date);
        
        LocalDate localDate = LocalDate.parse(date);
        LocalDateTime startOfDay = localDate.atStartOfDay();
        LocalDateTime endOfDay = localDate.plusDays(1).atStartOfDay();
        
        Double dailyEarnings = rideRepository.findByDriverIdAndStatus(driverId, "COMPLETED")
                .stream()
                .filter(ride -> ride.getEndTime() != null && 
                        !ride.getEndTime().isBefore(startOfDay) && 
                        ride.getEndTime().isBefore(endOfDay))
                .mapToDouble(ride -> ride.getPrice() != null ? ride.getPrice() : 0.0)
                .sum();
        
        return ResponseEntity.ok(dailyEarnings);
    }
    
    /**
     * Get current month earnings for a driver
     */
    @GetMapping("/{driverId}/earnings/monthly")
    public ResponseEntity<Double> getDriverMonthlyEarnings(@PathVariable Long driverId) {
        log.info("Fetching monthly earnings for driver: {}", driverId);
        
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.plusMonths(1).atDay(1).atStartOfDay();
        
        Double monthlyEarnings = rideRepository.findByDriverIdAndStatus(driverId, "COMPLETED")
                .stream()
                .filter(ride -> ride.getEndTime() != null && 
                        !ride.getEndTime().isBefore(startOfMonth) && 
                        ride.getEndTime().isBefore(endOfMonth))
                .mapToDouble(ride -> ride.getPrice() != null ? ride.getPrice() : 0.0)
                .sum();
        
        return ResponseEntity.ok(monthlyEarnings);
    }
    
    /**
     * Get current month completed rides count for a driver
     */
    @GetMapping("/{driverId}/monthly-rides-count")
    public ResponseEntity<Integer> getDriverMonthlyRidesCount(@PathVariable Long driverId) {
        log.info("Fetching monthly rides count for driver: {}", driverId);
        
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.plusMonths(1).atDay(1).atStartOfDay();
        
        int count = (int) rideRepository.findByDriverIdAndStatus(driverId, "COMPLETED")
                .stream()
                .filter(ride -> ride.getEndTime() != null && 
                        !ride.getEndTime().isBefore(startOfMonth) && 
                        ride.getEndTime().isBefore(endOfMonth))
                .count();
        
        return ResponseEntity.ok(count);
    }
}
