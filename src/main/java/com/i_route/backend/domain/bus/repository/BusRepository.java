package com.i_route.backend.domain.bus.repository;

import com.i_route.backend.domain.bus.entity.Bus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusRepository extends JpaRepository<Bus, Long> {
}
