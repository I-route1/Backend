package com.i_route.backend.gps.domain.gps.repository;

import com.i_route.backend.gps.domain.gps.entity.GpsEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GpsEventRepository extends JpaRepository<GpsEvent, Long> {
}
