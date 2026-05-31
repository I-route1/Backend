package com.i_route.backend.gps.domain.gps.service;

import com.i_route.backend.gps.domain.bus.entity.Bus;
import com.i_route.backend.gps.domain.bus.repository.BusRepository;
import com.i_route.backend.gps.domain.gps.dto.response.*;
import com.i_route.backend.gps.domain.gps.repository.CurrentLocationRedisRepository;
import com.i_route.backend.gps.domain.route.entity.RouteStop;
import com.i_route.backend.gps.domain.route.repository.RouteStopRepository;
import com.i_route.backend.gps.domain.route.service.RouteProgressService;
import com.i_route.backend.global.exception.CustomException;
import com.i_route.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GpsQueryService {

    private final BusRepository busRepository;
    private final RouteStopRepository routeStopRepository;
    private final CurrentLocationRedisRepository currentLocationRedisRepository;
    private final RouteProgressService routeProgressService;
    private final EtaService etaService;

    public CurrentBusLocationResponse getCurrentLocation(Long busId) {

        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new CustomException(ErrorCode.BUS_NOT_FOUND));

        if (!bus.isOperating()) {
            throw new CustomException(ErrorCode.BUS_NOT_OPERATING);
        }

        return currentLocationRedisRepository.findByBusId(busId)
                .orElseThrow(() -> new CustomException(ErrorCode.CURRENT_LOCATION_NOT_FOUND));
    }

    public BusRouteResponse getBusRoute(Long busId) {

        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new CustomException(ErrorCode.BUS_NOT_FOUND));

        CurrentBusLocationResponse current =
                currentLocationRedisRepository.findByBusId(busId)
                        .orElseThrow(() -> new CustomException(ErrorCode.CURRENT_LOCATION_NOT_FOUND));

        List<RouteStop> stops =
                routeStopRepository.findByRouteIdOrderByStopOrderAsc(
                        bus.getRoute().getId()
                );

        List<RouteStopResponse> stopResponses =
                routeProgressService.calculateStopStatuses(
                        current.getLatitude(),
                        current.getLongitude(),
                        stops
                );

        return BusRouteResponse.builder()
                .busId(bus.getId())
                .routeId(bus.getRoute().getId())
                .routeName(bus.getRoute().getRouteName())
                .busNumber(bus.getBusNumber())
                .driverName(bus.getDriver().getName())
                .driverPhoneNumber(bus.getDriver().getPhoneNumber())
                .stops(stopResponses)
                .build();
    }

    public List<EtaResponse> getStudentEtas(Long busId) {
        return etaService.calculateStudentEtas(busId);
    }
}