package com.i_route.backend.gps.domain.driver.repository;

import com.i_route.backend.gps.domain.driver.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, Long> {
}