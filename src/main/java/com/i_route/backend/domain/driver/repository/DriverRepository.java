package com.i_route.backend.domain.driver.repository;

import com.i_route.backend.domain.driver.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, Long> {
}
