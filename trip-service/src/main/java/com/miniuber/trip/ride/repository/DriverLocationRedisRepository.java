package com.miniuber.trip.ride.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis Repository for Geo-spatial queries
 * Manages driver location data using Redis GEO commands
 * 
 * Uses Redis Geo index to efficiently find drivers within a radius
 * Data structure: GEOADD driverlocation:active driverId latitude longitude
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DriverLocationRedisRepository {

    private final RedisTemplate<String, String> stringRedisTemplate;

    private static final String DRIVER_LOCATION_KEY = "driverlocation:active";
    private static final long LOCATION_CACHE_TTL = 3600;  // 1 hour

    /**
     * Add or update driver location in Redis Geo index
     * 
     * @param driverId Driver ID
     * @param latitude Driver's current latitude
     * @param longitude Driver's current longitude
     */
    public void addOrUpdateDriverLocation(Long driverId, Double latitude, Double longitude) {
        try {
            stringRedisTemplate.opsForGeo().add(
                    DRIVER_LOCATION_KEY,
                    new org.springframework.data.geo.Point(longitude, latitude),
                    driverId.toString()
            );

            log.debug("Updated driver {} location: {}, {}", driverId, latitude, longitude);
        } catch (Exception e) {
            log.error("Error updating driver location for driver {}", driverId, e);
        }
    }

    /**
     * Find all drivers within a given radius from a location
     * 
     * @param latitude Center point latitude (rider's pickup location)
     * @param longitude Center point longitude
     * @param radiusKm Search radius in kilometers
     * @return List of driver IDs within the radius
     */
    public List<Long> findDriversWithinRadius(Double latitude, Double longitude, Double radiusKm) {
        try {
            org.springframework.data.geo.Point center = new org.springframework.data.geo.Point(longitude, latitude);
            org.springframework.data.geo.Distance radius = new org.springframework.data.geo.Distance(radiusKm, 
                    org.springframework.data.geo.Metrics.KILOMETERS);
            org.springframework.data.geo.Circle circle = new org.springframework.data.geo.Circle(center, radius);

            GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().radius(
                    DRIVER_LOCATION_KEY,
                    circle
            );

            List<Long> result = new ArrayList<>();
            if (results != null) {
                results.getContent().forEach(geoResult -> {
                    String driverId = geoResult.getContent().getName();
                    result.add(Long.parseLong(driverId));
                });
            }

            log.debug("Found {} drivers within {} km of ({}, {})", 
                    result.size(), radiusKm, latitude, longitude);
            return result;
        } catch (Exception e) {
            log.error("Error finding drivers within radius", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get distance between driver and a location
     * 
     * @param driverId Driver ID
     * @param latitude Destination latitude
     * @param longitude Destination longitude
     * @return Distance in kilometers, or null if driver not found
     */
    public Double getDistanceToDriver(Long driverId, Double latitude, Double longitude) {
        try {
            org.springframework.data.geo.Point targetPoint = new org.springframework.data.geo.Point(longitude, latitude);

            org.springframework.data.geo.Distance distance = stringRedisTemplate.opsForGeo().distance(
                    DRIVER_LOCATION_KEY,
                    driverId.toString(),
                    driverId.toString(), // member name
                    org.springframework.data.geo.Metrics.KILOMETERS
            );

            return distance != null ? distance.getValue() : null;
        } catch (Exception e) {
            log.error("Error getting distance for driver {}", driverId, e);
            return null;
        }
    }

    /**
     * Get driver's current location from Redis
     * 
     * @param driverId Driver ID
     * @return Map with latitude and longitude, or empty map if not found
     */
    public Map<String, Double> getDriverLocation(Long driverId) {
        try {
            List<org.springframework.data.geo.Point> positions = stringRedisTemplate.opsForGeo().position(
                    DRIVER_LOCATION_KEY,
                    driverId.toString()
            );

            if (positions != null && !positions.isEmpty() && positions.get(0) != null) {
                org.springframework.data.geo.Point position = positions.get(0);
                return Map.of(
                        "latitude", position.getY(),
                        "longitude", position.getX()
                );
            }
            return Map.of();
        } catch (Exception e) {
            log.error("Error getting location for driver {}", driverId, e);
            return Map.of();
        }
    }

    /**
     * Remove driver from active location index (when driver goes offline)
     * 
     * @param driverId Driver ID
     */
    public void removeDriverLocation(Long driverId) {
        try {
            stringRedisTemplate.opsForSet().remove(DRIVER_LOCATION_KEY, driverId.toString());
            log.debug("Removed driver {} from location index", driverId);
        } catch (Exception e) {
            log.error("Error removing driver location for driver {}", driverId, e);
        }
    }

    /**
     * Get all active drivers in the system
     * 
     * @return List of all active driver IDs
     */
    public List<Long> getAllActiveDrivers() {
        try {
            Set<String> driverIds = stringRedisTemplate.opsForSet().members(DRIVER_LOCATION_KEY);
            List<Long> result = new ArrayList<>();
            if (driverIds != null) {
                driverIds.forEach(id -> result.add(Long.parseLong(id)));
            }
            return result;
        } catch (Exception e) {
            log.error("Error getting all active drivers", e);
            return new ArrayList<>();
        }
    }

    /**
     * Check if driver location exists in Redis
     * 
     * @param driverId Driver ID
     * @return true if driver is in active location index, false otherwise
     */
    public boolean isDriverActive(Long driverId) {
        try {
            return stringRedisTemplate.opsForSet().isMember(DRIVER_LOCATION_KEY, driverId.toString());
        } catch (Exception e) {
            log.error("Error checking if driver {} is active", driverId, e);
            return false;
        }
    }

    /**
     * Clear all driver locations (useful for testing or maintenance)
     */
    public void clearAllDriverLocations() {
        try {
            stringRedisTemplate.delete(DRIVER_LOCATION_KEY);
            log.info("Cleared all driver locations from Redis");
        } catch (Exception e) {
            log.error("Error clearing driver locations", e);
        }
    }
}
