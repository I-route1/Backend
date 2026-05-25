package com.i_route.backend.gps.domain.gps.repository;

import com.i_route.backend.gps.domain.gps.entity.BusLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusLocationRepository extends JpaRepository<BusLocation, Long> {

    List<BusLocation> findTop100ByBusIdOrderByRecordedAtDesc(Long busId);
}
