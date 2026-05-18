package com.i_route.backend.domain.gps.service;

import com.i_route.backend.domain.gps.dto.response.CurrentBusLocationResponse;
import com.i_route.backend.domain.gps.dto.response.EtaResponse;
import com.i_route.backend.domain.gps.repository.CurrentLocationRedisRepository;
import com.i_route.backend.domain.notification.service.NotificationService;
import com.i_route.backend.domain.route.entity.RouteStop;
import com.i_route.backend.domain.route.repository.RouteStopRepository;
import com.i_route.backend.domain.student.entity.Student;
import com.i_route.backend.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.i_route.backend.global.exception.CustomException;
import com.i_route.backend.global.exception.ErrorCode;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EtaService {

    private static final int LATE_THRESHOLD_MINUTES = 5;

    private final CurrentLocationRedisRepository currentLocationRepository;
    private final StudentRepository studentRepository;
    private final RouteStopRepository routeStopRepository;
    private final KakaoMobilityClient kakaoMobilityClient;
    private final NotificationService notificationService;

    public List<EtaResponse> calculateStudentEtas(Long busId) {
        CurrentBusLocationResponse currentLocation =
                currentLocationRepository.findByBusId(busId)
                        .orElseThrow(() -> new CustomException(ErrorCode.CURRENT_LOCATION_NOT_FOUND));
        List<Student> students = studentRepository.findByBusId(busId);

        return students.stream()
                .map(student -> calculateStudentEta(currentLocation, student))
                .toList();
    }

    private EtaResponse calculateStudentEta(
            CurrentBusLocationResponse currentLocation,
            Student student
    ) {
        RouteStop targetStop =
                routeStopRepository.findById(student.getRouteStopId())
                        .orElseThrow(() -> new CustomException(ErrorCode.ROUTE_STOP_NOT_FOUND));

        List<RouteStop> allStops =
                routeStopRepository.findByRouteIdOrderByStopOrderAsc(
                        targetStop.getRouteId()
                );

        List<RouteStop> waypoints =
                allStops.stream()
                        .filter(stop -> stop.getStopOrder() < targetStop.getStopOrder())
                        .sorted(Comparator.comparing(RouteStop::getStopOrder))
                        .toList();

        Integer durationSeconds =
                kakaoMobilityClient.getDurationSeconds(
                        currentLocation.getLatitude(),
                        currentLocation.getLongitude(),
                        targetStop,
                        waypoints
                );

        int etaMinutes = Math.max(1, durationSeconds / 60);

        int delayMinutes = calculateDelayMinutes(
                etaMinutes,
                student.getExpectedDropOffTime()
        );

        boolean lateRisk = delayMinutes >= LATE_THRESHOLD_MINUTES;

        if (lateRisk) {
            notificationService.sendLateRisk(
                    student.getId(),
                    delayMinutes
            );
        }

        return EtaResponse.builder()
                .studentId(student.getId())
                .studentName(student.getName())
                .stopId(targetStop.getId())
                .stopName(targetStop.getStopName())
                .etaMinutes(etaMinutes)
                .delayMinutes(delayMinutes)
                .lateRisk(lateRisk)
                .delayReason(resolveDelayReason(delayMinutes))
                .build();
    }

    private int calculateDelayMinutes(
            int etaMinutes,
            LocalTime expectedDropOffTime
    ) {
        LocalTime predictedArrival =
                LocalTime.now().plusMinutes(etaMinutes);

        return Math.max(
                0,
                predictedArrival.toSecondOfDay() / 60
                        - expectedDropOffTime.toSecondOfDay() / 60
        );
    }

    private String resolveDelayReason(int delayMinutes) {
        if (delayMinutes <= 0) {
            return "정상 운행";
        }

        if (delayMinutes >= 5) {
            return "교통 혼잡 또는 정차 지연";
        }

        return "약간 지연";
    }
}
