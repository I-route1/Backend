package com.i_route.backend.gps;

import com.i_route.backend.gps.domain.bus.entity.Bus;
import com.i_route.backend.gps.domain.bus.entity.OperationStatus;
import com.i_route.backend.gps.domain.bus.repository.BusRepository;
import com.i_route.backend.gps.domain.driver.entity.Driver;
import com.i_route.backend.gps.domain.gps.dto.response.CurrentBusLocationResponse;
import com.i_route.backend.gps.domain.gps.dto.response.EtaResponse;
import com.i_route.backend.gps.domain.gps.repository.CurrentLocationRedisRepository;
import com.i_route.backend.gps.domain.gps.service.EtaService;
import com.i_route.backend.gps.domain.gps.service.GpsQueryService;
import com.i_route.backend.gps.domain.route.repository.RouteStopRepository;
import com.i_route.backend.gps.domain.route.service.RouteProgressService;
import com.i_route.backend.gps.global.exception.CustomException;
import com.i_route.backend.gps.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class GpsQueryServiceTest {

    @InjectMocks
    private GpsQueryService gpsQueryService;

    @Mock private BusRepository busRepository;
    @Mock private RouteStopRepository routeStopRepository;
    @Mock private CurrentLocationRedisRepository currentLocationRedisRepository;
    @Mock private RouteProgressService routeProgressService;
    @Mock private EtaService etaService;

    private Bus operatingBus(Long id) {
        Driver driver = Driver.builder().id(1L).name("김기사").phoneNumber("010-1234-5678").build();
        return Bus.builder()
                .id(id)
                .busNumber("B-001")
                .operationStatus(OperationStatus.OPERATING)
                .driver(driver)
                .build();
    }

    private Bus stoppedBus(Long id) {
        Driver driver = Driver.builder().id(1L).name("김기사").phoneNumber("010-1234-5678").build();
        return Bus.builder()
                .id(id)
                .busNumber("B-001")
                .operationStatus(OperationStatus.READY)
                .driver(driver)
                .build();
    }

    private CurrentBusLocationResponse location(Long busId) {
        return CurrentBusLocationResponse.builder()
                .busId(busId)
                .latitude(37.5665)
                .longitude(126.9780)
                .speed(60.0)
                .heading(90.0)
                .updatedAt(LocalDateTime.now())
                .busNumber("B-001")
                .driverName("김기사")
                .driverPhoneNumber("010-1234-5678")
                .build();
    }

    // ── getCurrentLocation ───────────────────────────────────────

    @Test
    @DisplayName("현재 위치 조회 성공")
    void getCurrentLocation_success() {
        Bus bus = operatingBus(1L);
        CurrentBusLocationResponse loc = location(1L);
        given(busRepository.findById(1L)).willReturn(Optional.of(bus));
        given(currentLocationRedisRepository.findByBusId(1L)).willReturn(Optional.of(loc));

        CurrentBusLocationResponse result = gpsQueryService.getCurrentLocation(1L);

        assertThat(result.getBusId()).isEqualTo(1L);
        assertThat(result.getLatitude()).isEqualTo(37.5665);
    }

    @Test
    @DisplayName("현재 위치 조회 실패 - 버스 없음")
    void getCurrentLocation_busNotFound() {
        given(busRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> gpsQueryService.getCurrentLocation(99L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BUS_NOT_FOUND));
    }

    @Test
    @DisplayName("현재 위치 조회 실패 - 버스 운행 중 아님")
    void getCurrentLocation_busNotOperating() {
        given(busRepository.findById(1L)).willReturn(Optional.of(stoppedBus(1L)));

        assertThatThrownBy(() -> gpsQueryService.getCurrentLocation(1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BUS_NOT_OPERATING));
    }

    @Test
    @DisplayName("현재 위치 조회 실패 - Redis에 위치 없음")
    void getCurrentLocation_locationNotInRedis() {
        given(busRepository.findById(1L)).willReturn(Optional.of(operatingBus(1L)));
        given(currentLocationRedisRepository.findByBusId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> gpsQueryService.getCurrentLocation(1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CURRENT_LOCATION_NOT_FOUND));
    }

    // ── getStudentEtas ───────────────────────────────────────────

    @Test
    @DisplayName("학생 ETA 조회 - EtaService에 위임")
    void getStudentEtas_delegatesToEtaService() {
        List<EtaResponse> expected = List.of(mock(EtaResponse.class), mock(EtaResponse.class));
        given(etaService.calculateStudentEtas(1L)).willReturn(expected);

        List<EtaResponse> result = gpsQueryService.getStudentEtas(1L);

        assertThat(result).hasSize(2);
        then(etaService).should().calculateStudentEtas(1L);
    }
}
