/*package com.i_route.backend.gps.domain.gps.service;

import com.i_route.backend.gps.domain.bus.entity.Bus;
import com.i_route.backend.gps.domain.gps.dto.request.GpsLocationRequest;
import com.i_route.backend.gps.domain.gps.entity.GpsEvent;
import com.i_route.backend.gps.domain.gps.entity.GpsEventType;
import com.i_route.backend.gps.domain.gps.repository.GpsEventRepository;
import com.i_route.backend.gps.domain.gps.util.GeoUtil;
import com.i_route.backend.gps.domain.notification.service.NotificationService;
import com.i_route.backend.gps.domain.route.entity.RouteStop;
import com.i_route.backend.gps.domain.route.repository.RouteStopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteDeviationService {

    private static final double ROUTE_DEVIATION_THRESHOLD_METER = 100.0;

    private final RouteStopRepository routeStopRepository;
    private final GpsEventRepository gpsEventRepository;
    private final NotificationService notificationService;

    public void detect(
            Bus bus,
            GpsLocationRequest request
    ) {
        List<RouteStop> stops =
                routeStopRepository.findByRouteIdOrderByStopOrderAsc(
                        bus.getRoute().getId()
                );

        boolean normal = stops.stream()
                .anyMatch(stop ->
                        GeoUtil.distanceMeter(
                                request.getLatitude(),
                                request.getLongitude(),
                                stop.getLatitude(),
                                stop.getLongitude()
                        ) <= ROUTE_DEVIATION_THRESHOLD_METER
                );

        if (normal) {
            return;
        }

        gpsEventRepository.save(
                GpsEvent.builder()
                        .busId(bus.getId())
                        .eventType(GpsEventType.ROUTE_DEVIATION)
                        .message("등록된 노선에서 100m 이상 벗어났습니다.")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        notificationService.sendRouteDeviation(bus.getId());
    }
}*/
package com.i_route.backend.gps.domain.gps.service;

import com.i_route.backend.gps.domain.bus.entity.Bus;
import com.i_route.backend.gps.domain.gps.dto.request.GpsLocationRequest;
import com.i_route.backend.gps.domain.gps.entity.GpsEvent;
import com.i_route.backend.gps.domain.gps.entity.GpsEventType;
import com.i_route.backend.gps.domain.gps.repository.GpsEventRepository;
import com.i_route.backend.gps.domain.notification.service.NotificationService;
import com.i_route.backend.gps.domain.route.entity.RouteStop;
import com.i_route.backend.gps.domain.route.repository.RouteStopRepository;
import com.i_route.backend.gps.domain.route.service.RouteProjectionCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteDeviationService {
    private static final double ROUTE_DEVIATION_THRESHOLD_METER = 100.0;
    private final RouteStopRepository routeStopRepository;
    private final GpsEventRepository gpsEventRepository;
    private final NotificationService notificationService;
    private final RouteProjectionCalculator calculator;

    public void detect(Bus bus, GpsLocationRequest request) {
        List<RouteStop> stops = routeStopRepository.findByRouteIdOrderByStopOrderAsc(bus.getRoute().getId());
        if (stops.size() < 2) return;
        double distance = calculator.project(request.getLatitude(), request.getLongitude(), stops).distanceFromRouteMeters();
        if (distance <= ROUTE_DEVIATION_THRESHOLD_METER) return;
        // Event deduplication is separate from notification deduplication.
        gpsEventRepository.save(GpsEvent.builder().busId(bus.getId())
                .eventType(GpsEventType.ROUTE_DEVIATION)
                .message("등록된 노선에서 100m 이상 벗어났습니다.")
                .createdAt(LocalDateTime.now()).build());
        notificationService.sendRouteDeviation(bus.getId());
    }
}
