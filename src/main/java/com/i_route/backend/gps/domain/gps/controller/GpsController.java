package com.i_route.backend.gps.domain.gps.controller;

import com.i_route.backend.global.security.CustomUserDetails;
import com.i_route.backend.gps.domain.gps.dto.request.GpsLocationRequest;
import com.i_route.backend.gps.domain.gps.dto.response.BusRouteResponse;
import com.i_route.backend.gps.domain.gps.dto.response.CurrentBusLocationResponse;
import com.i_route.backend.gps.domain.gps.dto.response.EtaResponse;
import com.i_route.backend.gps.domain.gps.dto.response.PassengerResponse;
import com.i_route.backend.gps.domain.gps.service.GpsCommandService;
import com.i_route.backend.gps.domain.gps.service.GpsQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gps")
@RequiredArgsConstructor
public class GpsController {

    private final GpsCommandService gpsCommandService;
    private final GpsQueryService gpsQueryService;

    // 기사 앱: 5초 주기 GPS 전송
    @PostMapping("/locations")
    public void receiveLocation(
            @Valid @RequestBody GpsLocationRequest request
    ) {
        gpsCommandService.receiveLocation(request);
    }

    // 학부모 앱: 현재 차량 위치 조회
    @GetMapping("/buses/{busId}/current-location")
    public CurrentBusLocationResponse getCurrentLocation(
            @PathVariable Long busId
    ) {
        return gpsQueryService.getCurrentLocation(busId);
    }

    // 학부모 앱: 차량 노선, 정류장 진행 상태, 기사 정보 조회
    @GetMapping("/buses/{busId}/route")
    public BusRouteResponse getBusRoute(
            @PathVariable Long busId
    ) {
        return gpsQueryService.getBusRoute(busId);
    }

    // 학부모 앱: 학생별 ETA, 지각 위험, 지연 원인 조회
    @GetMapping("/buses/{busId}/etas")
    public List<EtaResponse> getEtas(
            @PathVariable Long busId
    ) {
        return gpsQueryService.getStudentEtas(busId);
    }

    // 학부모 앱: 학생 탑승 중일 때만 버스 GPS 반환 (하차 후 204)
    @GetMapping("/students/{studentId}/current-location")
    public ResponseEntity<CurrentBusLocationResponse> getStudentCurrentLocation(
            @PathVariable Long studentId
    ) {
        return gpsQueryService.getStudentCurrentLocation(studentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    // 기사 앱: 오늘 배정 탑승 명단 조회
    @GetMapping("/buses/{busId}/passengers/today")
    public List<PassengerResponse> getTodayPassengers(
            @PathVariable Long busId
    ) {
        return gpsQueryService.getTodayPassengers(busId);
    }

    // 기사 앱: 자신의 버스 탑승 명단 조회 (학원 정보 포함)
    @GetMapping("/drivers/my-bus/attendance")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<PassengerResponse>> getMyBusAttendance(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<PassengerResponse> passengers = gpsQueryService.getDriverBusAttendance(userDetails.getId());
        return ResponseEntity.ok(passengers);
    }
}
