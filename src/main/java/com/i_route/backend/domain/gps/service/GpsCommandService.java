package com.i_route.backend.domain.gps.service;

import com.i_route.backend.domain.bus.entity.Bus;
import com.i_route.backend.domain.bus.repository.BusRepository;
import com.i_route.backend.domain.gps.dto.request.GpsLocationRequest;
import com.i_route.backend.domain.gps.dto.response.CurrentBusLocationResponse;
import com.i_route.backend.domain.gps.entity.BusLocation;
import com.i_route.backend.domain.gps.repository.BusLocationRepository;
import com.i_route.backend.domain.gps.repository.CurrentLocationRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.i_route.backend.global.exception.CustomException;
import com.i_route.backend.global.exception.ErrorCode;


import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class GpsCommandService {

    private final BusRepository busRepository;
    private final BusLocationRepository busLocationRepository;
    private final CurrentLocationRedisRepository currentLocationRedisRepository;
    private final RouteDeviationService routeDeviationService;
    private final StopDetectionService stopDetectionService;
    private final SimpMessagingTemplate messagingTemplate;

    public void receiveLocation(GpsLocationRequest request) {

        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new CustomException(ErrorCode.BUS_NOT_FOUND));

        if (!bus.isOperating()) {
            throw new CustomException(ErrorCode.BUS_NOT_OPERATING);
        }

        BusLocation savedLocation =
                busLocationRepository.save(
                        BusLocation.builder()
                                .busId(bus.getId())
                                .latitude(request.getLatitude())
                                .longitude(request.getLongitude())
                                .speed(request.getSpeed())
                                .heading(request.getHeading())
                                .recordedAt(LocalDateTime.now())
                                .build()
                );

        CurrentBusLocationResponse response =
                CurrentBusLocationResponse.builder()
                        .busId(bus.getId())
                        .latitude(savedLocation.getLatitude())
                        .longitude(savedLocation.getLongitude())
                        .speed(savedLocation.getSpeed())
                        .heading(savedLocation.getHeading())
                        .updatedAt(savedLocation.getRecordedAt())
                        .busNumber(bus.getBusNumber())
                        .driverName(bus.getDriver().getName())
                        .driverPhoneNumber(bus.getDriver().getPhoneNumber())
                        .build();

        currentLocationRedisRepository.save(response);

        routeDeviationService.detect(bus, request);

        stopDetectionService.detect(request);

        messagingTemplate.convertAndSend(
                "/topic/bus/" + bus.getId(),
                response
        );
    }
}
