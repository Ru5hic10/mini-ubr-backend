package com.miniuber.core.driver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniuber.core.driver.entity.DriverEarnings;
import com.miniuber.core.driver.repository.DriverEarningsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverEarningsService {

    private final DriverNotificationService notificationService;
    private final DriverEarningsRepository earningsRepository;
    private final ObjectMapper objectMapper;

    private static final double COMMISSION_PERCENT = 0.20; // 20%

    /**
     * Listen for completed ride events and calculate earnings.
     * Topic: rides.completed
     * Expected payload: {"rideId":123,"driverId":456,"grossAmount":250.0}
     */
    @KafkaListener(topics = "rides.completed", groupId = "driver-service-group", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleRideCompleted(String message) {
        try {
            Map<String, Object> rideEvent = objectMapper.readValue(message, Map.class);

            Long rideId = ((Number) rideEvent.get("rideId")).longValue();
            Long driverId = ((Number) rideEvent.get("driverId")).longValue();
            Double grossAmount = ((Number) rideEvent.get("grossAmount")).doubleValue();

            log.info("Calculating earnings for ride {} - gross amount: ₹{}", rideId, grossAmount);

            Double commissionAmount = grossAmount * COMMISSION_PERCENT;
            Double netAmount = grossAmount - commissionAmount;

            DriverEarnings earnings = DriverEarnings.builder()
                    .driverId(driverId)
                    .rideId(rideId)
                    .grossAmount(grossAmount)
                    .commissionPercent(COMMISSION_PERCENT * 100)
                    .commissionAmount(commissionAmount)
                    .netAmount(netAmount)
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();

            earningsRepository.save(earnings);

            log.info("Earnings saved for driver {} on ride {}: gross=₹{}, commission=₹{}, net=₹{}",
                    driverId, rideId, grossAmount, commissionAmount, netAmount);

            notificationService.notifyEarningsCredited(driverId, netAmount, commissionAmount);
        } catch (Exception e) {
            log.error("Error calculating earnings from Kafka event", e);
        }
    }

    /**
     * Get today's earnings summary for a driver.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getEarningsSummary(Long driverId) {
        log.info("Fetching earnings summary for driver {}", driverId);

        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<DriverEarnings> earnings = earningsRepository.findByDriverIdAndCreatedAtBetween(driverId, start, end);

        double totalGross = earnings.stream().mapToDouble(e -> e.getGrossAmount() != null ? e.getGrossAmount() : 0.0).sum();
        double totalCommission = earnings.stream().mapToDouble(e -> e.getCommissionAmount() != null ? e.getCommissionAmount() : 0.0).sum();
        double totalNet = earnings.stream().mapToDouble(e -> e.getNetAmount() != null ? e.getNetAmount() : 0.0).sum();
        int totalRides = earnings.size();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRides", totalRides);
        summary.put("totalGrossEarnings", totalGross);
        summary.put("totalCommission", totalCommission);
        summary.put("totalNetEarnings", totalNet);
        summary.put("averagePerRide", totalRides == 0 ? 0.0 : totalNet / totalRides);
        summary.put("date", today.toString());

        return summary;
    }

    /**
     * Get paginated earnings history for a driver.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEarningsHistory(Long driverId, int limit, int offset) {
        log.info("Fetching earnings history for driver {} (limit: {}, offset: {})", driverId, limit, offset);

        int page = offset / limit;
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        return earningsRepository.findByDriverIdOrderByCreatedAtDesc(driverId, pageable)
                .stream()
                .map(this::toHistoryMap)
                .collect(Collectors.toList());
    }

    /**
     * Get weekly earnings trend (last 7 days) grouped by date.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWeeklyEarningsTrend(Long driverId) {
        log.info("Fetching weekly earnings trend for driver {}", driverId);

        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(6).atStartOfDay();
        List<DriverEarnings> earnings = earningsRepository
                .findByDriverIdAndCreatedAtAfterOrderByCreatedAtDesc(driverId, sevenDaysAgo);

        Map<LocalDate, List<DriverEarnings>> grouped = earnings.stream()
                .collect(Collectors.groupingBy(e -> e.getCreatedAt().toLocalDate()));

        List<Map<String, Object>> trend = new ArrayList<>();
        grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    LocalDate date = entry.getKey();
                    List<DriverEarnings> daily = entry.getValue();
                    double total = daily.stream().mapToDouble(e -> e.getNetAmount() != null ? e.getNetAmount() : 0.0).sum();
                    int rides = daily.size();

                    Map<String, Object> day = new HashMap<>();
                    day.put("date", date.toString());
                    day.put("totalRides", rides);
                    day.put("totalEarnings", total);
                    day.put("averagePerRide", rides == 0 ? 0.0 : total / rides);
                    trend.add(day);
                });

        return trend;
    }

    /**
     * Get monthly earnings statistics for the current month.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMonthlyEarnings(Long driverId) {
        log.info("Fetching monthly earnings for driver {}", driverId);

        YearMonth month = YearMonth.now();
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();

        List<DriverEarnings> earnings = earningsRepository.findByDriverIdAndCreatedAtBetween(driverId, start, end);

        double totalGross = earnings.stream().mapToDouble(e -> e.getGrossAmount() != null ? e.getGrossAmount() : 0.0).sum();
        double totalCommission = earnings.stream().mapToDouble(e -> e.getCommissionAmount() != null ? e.getCommissionAmount() : 0.0).sum();
        double totalNet = earnings.stream().mapToDouble(e -> e.getNetAmount() != null ? e.getNetAmount() : 0.0).sum();
        int totalRides = earnings.size();

        Map<String, Object> summary = new HashMap<>();
        summary.put("month", month.getMonth().toString() + " " + month.getYear());
        summary.put("totalRides", totalRides);
        summary.put("totalGrossEarnings", totalGross);
        summary.put("totalCommission", totalCommission);
        summary.put("totalNetEarnings", totalNet);
        summary.put("averagePerRide", totalRides == 0 ? 0.0 : totalNet / totalRides);

        Map<LocalDate, Double> byDate = earnings.stream()
                .collect(Collectors.groupingBy(e -> e.getCreatedAt().toLocalDate(),
                        Collectors.summingDouble(e -> e.getNetAmount() != null ? e.getNetAmount() : 0.0)));

        byDate.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> {
                    summary.put("bestDay", entry.getKey().toString());
                    summary.put("bestDayEarnings", entry.getValue());
                });

        return summary;
    }

    /**
     * Mark earnings as paid when payout is processed.
     */
    @Transactional
    public void markEarningsAsPaid(Long earningsId) {
        log.info("Marking earnings {} as paid", earningsId);
        earningsRepository.updateEarningsStatus(earningsId, "PAID", LocalDateTime.now());
    }

    private Map<String, Object> toHistoryMap(DriverEarnings earnings) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", earnings.getId());
        map.put("rideId", earnings.getRideId());
        map.put("grossAmount", earnings.getGrossAmount());
        map.put("commissionAmount", earnings.getCommissionAmount());
        map.put("netAmount", earnings.getNetAmount());
        map.put("status", earnings.getStatus());
        map.put("createdAt", earnings.getCreatedAt());
        map.put("paidAt", earnings.getPaidAt());
        return map;
    }
}
