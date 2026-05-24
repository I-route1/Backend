package com.i_route.backend.gps.domain.route.service;

import com.i_route.backend.gps.domain.gps.dto.response.RouteStopResponse;
import com.i_route.backend.gps.domain.gps.util.GeoUtil;
import com.i_route.backend.gps.domain.route.entity.RouteStop;
import com.i_route.backend.gps.domain.route.entity.StopStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RouteProgressService {

    private static final double ARRIVAL_RADIUS_METER = 50.0;

    public List<RouteStopResponse> calculateStopStatuses(
            Double busLat,
            Double busLng,
            List<RouteStop> stops
    ) {
        int nearestIndex = findNearestStopIndex(busLat, busLng, stops);

        List<RouteStopResponse> responses = new ArrayList<>();

        for (int i = 0; i < stops.size(); i++) {
            RouteStop stop = stops.get(i);

            StopStatus status;

            double distance = GeoUtil.distanceMeter(
                    busLat,
                    busLng,
                    stop.getLatitude(),
                    stop.getLongitude()
            );

            if (distance <= ARRIVAL_RADIUS_METER) {
                status = StopStatus.ARRIVED;
            } else if (i < nearestIndex) {
                status = StopStatus.PASSED;
            } else {
                status = StopStatus.BEFORE_ARRIVAL;
            }

            responses.add(
                    RouteStopResponse.builder()
                            .stopId(stop.getId())
                            .stopName(stop.getStopName())
                            .latitude(stop.getLatitude())
                            .longitude(stop.getLongitude())
                            .stopOrder(stop.getStopOrder())
                            .status(status)
                            .etaMinutes(null)
                            .build()
            );
        }

        return responses;
    }

    private int findNearestStopIndex(
            Double busLat,
            Double busLng,
            List<RouteStop> stops
    ) {
        int nearestIndex = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < stops.size(); i++) {
            RouteStop stop = stops.get(i);

            double distance = GeoUtil.distanceMeter(
                    busLat,
                    busLng,
                    stop.getLatitude(),
                    stop.getLongitude()
            );

            if (distance < minDistance) {
                minDistance = distance;
                nearestIndex = i;
            }
        }

        return nearestIndex;
    }
}
