package com.i_route.backend.gps.domain.route.repository;

import com.i_route.backend.gps.domain.route.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Long> {
}
