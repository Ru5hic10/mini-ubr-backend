package com.miniuber.core.driver.repository;

import com.miniuber.core.driver.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByLicenseNumber(String licenseNumber);

    @Query("SELECT d FROM Driver d WHERE d.available = true")
    List<Driver> findAllAvailableDrivers();
}