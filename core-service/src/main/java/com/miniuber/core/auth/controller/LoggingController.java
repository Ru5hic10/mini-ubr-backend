package com.miniuber.core.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LoggingController {

    private static final List<String> logs = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

    static {
        addLog("System initialized successfully.");
        addLog("Auth Service started on port 8081");
        addLog("Connected to Eureka Server at eureka-server:8761");
        addLog("Kafka consumer groups registered: ride-service-group, user-service-group");
        addLog("Database 'core_db' connected and schema verified.");
        addLog("Dummy log seed completed.");
    }

    @GetMapping
    public ResponseEntity<List<String>> getLogs() {
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<String>> getRecentLogs(@RequestParam(defaultValue = "10") int limit) {
        int startIndex = Math.max(0, logs.size() - limit);
        return ResponseEntity.ok(logs.subList(startIndex, logs.size()));
    }

    @PostMapping("/clear")
    public ResponseEntity<Map<String, String>> clearLogs() {
        int size = logs.size();
        logs.clear();
        return ResponseEntity.ok(Map.of("message", "Logs cleared", "count", String.valueOf(size)));
    }

    public static void addLog(String message) {
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new java.util.Date());
        logs.add("[" + timestamp + "] " + message);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getLogCount() {
        return ResponseEntity.ok(Map.of("count", logs.size()));
    }

    // Expose current root log level
    @GetMapping("/level")
    public ResponseEntity<Map<String, String>> getLogLevel() {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        String level = "INFO";
        try {
            if (logger instanceof ch.qos.logback.classic.Logger logbackLogger) {
                ch.qos.logback.classic.Level lvl = logbackLogger.getLevel();
                level = (lvl != null) ? lvl.levelStr : level;
            }
        } catch (Throwable ignored) {
        }
        return ResponseEntity.ok(Map.of("level", level));
    }

    // Set root log level (DEBUG, INFO, WARN, ERROR)
    @PostMapping("/level")
    public ResponseEntity<Map<String, String>> setLogLevel(@RequestBody Map<String, String> body) {
        String requested = body.getOrDefault("level", "INFO").toUpperCase();
        try {
            org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            if (logger instanceof ch.qos.logback.classic.Logger logbackLogger) {
                ch.qos.logback.classic.Level lvl = ch.qos.logback.classic.Level.toLevel(requested,
                        ch.qos.logback.classic.Level.INFO);
                logbackLogger.setLevel(lvl);
            }
            return ResponseEntity.ok(Map.of("level", requested, "status", "updated"));
        } catch (Throwable e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid level", "requested", requested));
        }
    }
}
