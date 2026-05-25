package com.miniuber.core.driver.controller;

import com.miniuber.core.driver.service.DriverEarningsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Driver Earnings Controller
 * REST endpoints for earnings tracking and reporting
 */
@Slf4j
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverEarningsController {

    private final DriverEarningsService earningsService;

    /**
     * GET /api/drivers/{driverId}/earnings/summary
     * Get earnings summary for today
     * 
     * Response:
     * {
     *   "totalRides": 5,
     *   "totalGrossEarnings": 1250.00,
     *   "totalCommission": 250.00,
     *   "totalNetEarnings": 1000.00,
     *   "averagePerRide": 250.00,
     *   "date": "2026-01-11"
     * }
     * 
     * @param driverId Driver ID
     * @return Today's earnings summary
     */
    @GetMapping("/{driverId}/earnings/summary")
    public ResponseEntity<Map<String, Object>> getEarningsSummary(@PathVariable Long driverId) {
        log.info("Fetching earnings summary for driver {}", driverId);

        try {
            Map<String, Object> summary = earningsService.getEarningsSummary(driverId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error fetching earnings summary", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/drivers/{driverId}/earnings/history
     * Get detailed earnings history with pagination
     * 
     * Query parameters:
     * - limit: Number of records (default: 20, max: 100)
     * - page: Page number (default: 0)
     * 
     * Response is list of:
     * {
     *   "rideId": 123,
     *   "grossAmount": 250.00,
     *   "commissionAmount": 50.00,
     *   "netAmount": 200.00,
     *   "pickupLocation": "123 Main St",
     *   "dropoffLocation": "456 Oak Ave",
     *   "distance": 12.5,
     *   "startTime": "2026-01-11T10:30:00",
     *   "endTime": "2026-01-11T10:45:00"
     * }
     * 
     * @param driverId Driver ID
     * @param limit Number of records per page
     * @param page Page number
     * @return List of earnings records
     */
    @GetMapping("/{driverId}/earnings/history")
    public ResponseEntity<List<Map<String, Object>>> getEarningsHistory(
            @PathVariable Long driverId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int page) {

        log.info("Fetching earnings history for driver {} (limit: {}, page: {})", 
                driverId, limit, page);

        try {
            // Cap limit at 100
            if (limit > 100) limit = 100;

            int offset = page * limit;
            List<Map<String, Object>> history = earningsService.getEarningsHistory(driverId, limit, offset);

            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching earnings history", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/drivers/{driverId}/earnings/weekly
     * Get weekly earnings trend (last 7 days)
     * 
     * Response is list of daily earnings:
     * {
     *   "date": "2026-01-11",
     *   "totalRides": 5,
     *   "totalEarnings": 1000.00,
     *   "averagePerRide": 200.00
     * }
     * 
     * @param driverId Driver ID
     * @return Weekly earnings trend
     */
    @GetMapping("/{driverId}/earnings/weekly")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyEarningsTrend(@PathVariable Long driverId) {
        log.info("Fetching weekly earnings trend for driver {}", driverId);

        try {
            List<Map<String, Object>> trend = earningsService.getWeeklyEarningsTrend(driverId);
            return ResponseEntity.ok(trend);
        } catch (Exception e) {
            log.error("Error fetching weekly earnings trend", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Alias used by test script: /earnings/weekly-trend
    @GetMapping("/{driverId}/earnings/weekly-trend")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyEarningsTrendAlias(@PathVariable Long driverId) {
        return getWeeklyEarningsTrend(driverId);
    }

    /**
     * GET /api/drivers/{driverId}/earnings/monthly
     * Get monthly earnings statistics
     * 
     * Response:
     * {
     *   "month": "January 2026",
     *   "totalRides": 150,
     *   "totalGrossEarnings": 37500.00,
     *   "totalCommission": 7500.00,
     *   "totalNetEarnings": 30000.00,
     *   "averagePerRide": 250.00,
     *   "bestDay": "2026-01-11",
     *   "bestDayEarnings": 1500.00
     * }
     * 
     * @param driverId Driver ID
     * @return Monthly earnings statistics
     */
    @GetMapping("/{driverId}/earnings/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlyEarnings(@PathVariable Long driverId) {
        log.info("Fetching monthly earnings for driver {}", driverId);

        // Placeholder implementation
        Map<String, Object> monthlyEarnings = Map.of(
                "month", "January 2026",
                "totalRides", 150,
                "totalGrossEarnings", 37500.00,
                "totalCommission", 7500.00,
                "totalNetEarnings", 30000.00,
                "averagePerRide", 250.00
        );

        return ResponseEntity.ok(monthlyEarnings);
    }
}
