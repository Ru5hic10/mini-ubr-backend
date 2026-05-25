package com.miniuber.core.driver.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Map;

/**
 * Feign client for communicating with ride-service
 * Uses Eureka service discovery to locate ride-service instances
 */
@FeignClient(name = "ride-service")
public interface RideServiceClient {
    
    /**
     * Get total earnings for a driver
     */
    @GetMapping("/api/rides/driver/{driverId}/earnings/total")
    Double getDriverTotalEarnings(@PathVariable("driverId") Long driverId);
    
    /**
     * Get completed rides count for a driver
     */
    @GetMapping("/api/rides/driver/{driverId}/completed-count")
    Integer getDriverCompletedRidesCount(@PathVariable("driverId") Long driverId);
    
    /**
     * Get daily earnings for a driver
     */
    @GetMapping("/api/rides/driver/{driverId}/earnings/daily")
    Double getDriverDailyEarnings(
        @PathVariable("driverId") Long driverId,
        @RequestParam("date") String date
    );
    
    /**
     * Get current month earnings for a driver
     */
    @GetMapping("/api/rides/driver/{driverId}/earnings/monthly")
    Double getDriverMonthlyEarnings(@PathVariable("driverId") Long driverId);
    
    /**
     * Get current month completed rides count for a driver
     */
    @GetMapping("/api/rides/driver/{driverId}/monthly-rides-count")
    Integer getDriverMonthlyRidesCount(@PathVariable("driverId") Long driverId);
}
