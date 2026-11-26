package com.i_route.backend.gps.domain.route.service;

import com.i_route.backend.gps.domain.gps.dto.response.RouteStopResponse;
import com.i_route.backend.gps.domain.gps.service.RoadProjectionService;
import com.i_route.backend.gps.domain.gps.util.GeoUtil;
import com.i_route.backend.gps.domain.route.entity.RouteStop;
import com.i_route.backend.gps.domain.route.entity.StopStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RouteProgressService {
    private static final double ARRIVAL_METERS=50;
    private static final double MAX_OFFSET_METERS=100;
    private final RoadProjectionService projector;

    public List<RouteStopResponse> calculateStopStatuses(Double lat, Double lng, List<RouteStop> stops) {
        if (stops.isEmpty()) return List.of();
        if (lat == null || lng == null) {
            return stops.stream().map(stop -> response(stop, StopStatus.BEFORE_ARRIVAL)).toList();
        }
        Long routeId=stops.get(0).getRouteId();
        RoadProjectionService.Projection bus=projector.project(routeId,lat,lng);
        // Do not claim PASSED from an off-route location.
        boolean onRoute=bus.offsetMeters()<=MAX_OFFSET_METERS;
        List<RouteStopResponse> out=new ArrayList<>();
        for(RouteStop stop:stops) {
            double direct=GeoUtil.distanceMeter(lat,lng,stop.getLatitude(),stop.getLongitude());
            double stopProgress=projector.project(routeId,stop.getLatitude(),stop.getLongitude()).progressMeters();
            StopStatus status = onRoute && direct<=ARRIVAL_METERS ? StopStatus.ARRIVED
                    : onRoute && stopProgress<bus.progressMeters() ? StopStatus.PASSED
                    : StopStatus.BEFORE_ARRIVAL;
            out.add(response(stop, status));
        }
        return out;
    }

    private RouteStopResponse response(RouteStop stop, StopStatus status) {
        return RouteStopResponse.builder()
                .stopId(stop.getId())
                .stopName(stop.getStopName())
                .latitude(stop.getLatitude())
                .longitude(stop.getLongitude())
                .stopOrder(stop.getStopOrder())
                .scheduledArrivalTime(stop.getScheduledArrivalTime())
                .status(status)
                .etaMinutes(null)
                .build();
    }
}
