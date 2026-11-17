package com.i_route.backend.gps.domain.gps.service;

import com.i_route.backend.gps.domain.bus.entity.Bus;
import com.i_route.backend.gps.domain.gps.dto.request.GpsLocationRequest;
import com.i_route.backend.gps.domain.gps.entity.GpsEvent;
import com.i_route.backend.gps.domain.gps.entity.GpsEventType;
import com.i_route.backend.gps.domain.gps.repository.GpsEventRepository;
import com.i_route.backend.gps.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RouteDeviationService {
    private static final double LIMIT_METERS=100;
    private final RoadProjectionService projector;
    private final GpsEventRepository events;
    private final NotificationService notifications;
    private final ConcurrentHashMap<Long,Boolean> previousDeviation = new ConcurrentHashMap<>();

    public void detect(Bus bus, GpsLocationRequest request) {
        double offset=projector.project(bus.getRoute().getId(),request.getLatitude(),request.getLongitude()).offsetMeters();
        boolean deviated=offset>LIMIT_METERS;
        boolean previously=Boolean.TRUE.equals(previousDeviation.put(bus.getId(),deviated));
        if(!deviated || previously) return;
        events.save(GpsEvent.builder().busId(bus.getId())
                .eventType(GpsEventType.ROUTE_DEVIATION)
                .message("등록된 도로 경로에서 100m 이상 벗어났습니다.")
                .createdAt(LocalDateTime.now()).build());
        notifications.sendRouteDeviation(bus.getId());
    }
}
