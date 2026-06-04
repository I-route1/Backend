package com.i_route.backend.gps.domain.gps.service;

import com.i_route.backend.gps.domain.attendance.entity.Attendance;
import com.i_route.backend.gps.domain.attendance.entity.AttendanceEventType;
import com.i_route.backend.gps.domain.attendance.repository.AttendanceRepository;
import com.i_route.backend.gps.domain.attendance.repository.StudentBoardingRedisRepository;
import com.i_route.backend.gps.domain.bus.entity.Bus;
import com.i_route.backend.gps.domain.bus.repository.BusRepository;
import com.i_route.backend.gps.domain.gps.dto.response.*;
import com.i_route.backend.gps.domain.gps.repository.CurrentLocationRedisRepository;
import com.i_route.backend.gps.domain.route.entity.RouteStop;
import com.i_route.backend.gps.domain.route.repository.RouteStopRepository;
import com.i_route.backend.gps.domain.route.service.RouteProgressService;
import com.i_route.backend.gps.domain.student.entity.PassengerStatus;
import com.i_route.backend.gps.domain.student.entity.Student;
import com.i_route.backend.gps.domain.student.repository.StudentRepository;
import com.i_route.backend.global.exception.CustomException;
import com.i_route.backend.global.exception.ErrorCode;
import com.i_route.backend.user.entity.Academy;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.AcademyRepository;
import com.i_route.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GpsQueryService {

    private final BusRepository busRepository;
    private final RouteStopRepository routeStopRepository;
    private final CurrentLocationRedisRepository currentLocationRedisRepository;
    private final RouteProgressService routeProgressService;
    private final EtaService etaService;
    private final StudentBoardingRedisRepository studentBoardingRedisRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AcademyRepository academyRepository;
    private final UserRepository userRepository;

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

    // 학생 탑승 중일 때만 버스 GPS 반환 (하차 후에는 Optional.empty())
    public Optional<CurrentBusLocationResponse> getStudentCurrentLocation(Long studentId) {
        return studentBoardingRedisRepository.getBusId(studentId)
                .flatMap(currentLocationRedisRepository::findByBusId);
    }

    // 기사의 오늘 배정 탑승 명단 조회
    public List<PassengerResponse> getTodayPassengers(Long busId) {
        busRepository.findById(busId)
                .orElseThrow(() -> new CustomException(ErrorCode.BUS_NOT_FOUND));

        List<Student> students = studentRepository.findByBusId(busId);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<Attendance> todayAttendances = attendanceRepository
                .findByBusIdAndTimestampBetweenOrderByTimestampAsc(busId, startOfDay, endOfDay);

        // 학생별 가장 최근 이벤트 (오늘)
        Map<Long, AttendanceEventType> latestEventByStudent = todayAttendances.stream()
                .collect(Collectors.toMap(
                        Attendance::getStudentId,
                        Attendance::getEventType,
                        (existing, replacement) -> replacement
                ));

        return students.stream()
                .map(student -> {
                    RouteStop stop = student.getRouteStopId() != null
                            ? routeStopRepository.findById(student.getRouteStopId()).orElse(null)
                            : null;

                    AttendanceEventType latestEvent = latestEventByStudent.get(student.getStudentId());
                    PassengerStatus status;
                    if (latestEvent == null) {
                        status = PassengerStatus.WAITING;
                    } else if (latestEvent == AttendanceEventType.BOARD) {
                        status = PassengerStatus.BOARDED;
                    } else {
                        status = PassengerStatus.EXITED;
                    }

                    String academyName = null;
                    if (student.getAcademyId() != null) {
                        Academy academy = academyRepository.findById(student.getAcademyId()).orElse(null);
                        if (academy != null) {
                            academyName = academy.getAcademyName();
                        }
                    }

                    return PassengerResponse.builder()
                            .studentId(student.getStudentId())
                            .name(student.getName())
                            .profileImage(student.getProfileImage())
                            .stopName(stop != null ? stop.getStopName() : null)
                            .status(status)
                            .academyId(student.getAcademyId())
                            .academyName(academyName)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // 기사의 학원 학생 탑승 현황 조회
    public List<PassengerResponse> getDriverBusAttendance(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return List.of();
        }

        Academy academy = academyRepository.findByDriver_UserId(userId).orElse(null);
        if (academy == null) {
            return List.of();
        }

        Long academyId = academy.getAcademyId();

        // 학원의 모든 학생 조회
        List<Student> students = studentRepository.findAll().stream()
                .filter(s -> academyId.equals(s.getAcademyId()))
                .toList();

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<Attendance> todayAttendances = attendanceRepository
                .findByTimestampBetweenOrderByTimestampAsc(startOfDay, endOfDay);

        // 학생별 가장 최근 이벤트
        Map<Long, AttendanceEventType> latestEventByStudent = todayAttendances.stream()
                .collect(Collectors.toMap(
                        Attendance::getStudentId,
                        Attendance::getEventType,
                        (existing, replacement) -> replacement
                ));

        return students.stream()
                .map(student -> {
                    RouteStop stop = student.getRouteStopId() != null
                            ? routeStopRepository.findById(student.getRouteStopId()).orElse(null)
                            : null;

                    AttendanceEventType latestEvent = latestEventByStudent.get(student.getStudentId());
                    PassengerStatus status;
                    if (latestEvent == null) {
                        status = PassengerStatus.WAITING;
                    } else if (latestEvent == AttendanceEventType.BOARD) {
                        status = PassengerStatus.BOARDED;
                    } else {
                        status = PassengerStatus.EXITED;
                    }

                    return PassengerResponse.builder()
                            .studentId(student.getStudentId())
                            .name(student.getName())
                            .profileImage(student.getProfileImage())
                            .stopName(stop != null ? stop.getStopName() : null)
                            .status(status)
                            .academyId(student.getAcademyId())
                            .academyName(academy.getAcademyName())
                            .build();
                })
                .collect(Collectors.toList());
    }
}