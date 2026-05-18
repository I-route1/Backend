package com.i_route.backend.domain.gps.repository;

import com.i_route.backend.domain.gps.entity.GpsEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GpsEventRepository extends JpaRepository<GpsEvent, Long> {
}
