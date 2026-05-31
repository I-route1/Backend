package com.i_route.backend.gps;

import com.i_route.backend.gps.domain.bus.entity.Bus;
import com.i_route.backend.gps.domain.bus.entity.OperationStatus;
import com.i_route.backend.gps.domain.bus.repository.BusRepository;
import com.i_route.backend.gps.domain.driver.entity.Driver;
import com.i_route.backend.gps.domain.gps.dto.request.GpsLocationRequest;
import com.i_route.backend.gps.domain.gps.entity.BusLocation;
import com.i_route.backend.gps.domain.gps.repository.BusLocationRepository;
import com.i_route.backend.gps.domain.gps.repository.CurrentLocationRedisRepository;
import com.i_route.backend.gps.domain.gps.service.GpsCommandService;
import com.i_route.backend.gps.domain.gps.service.RouteDeviationService;
import com.i_route.backend.gps.domain.gps.service.StopDetectionService;
import com.i_route.backend.gps.global.exception.CustomException;
import com.i_route.backend.gps.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class GpsCommandServiceTest {

    @InjectMocks
    private GpsCommandService gpsCommandService;

    @Mock private BusRepository busRepository;
    @Mock private BusLocationRepository busLocationRepository;
    @Mock private CurrentLocationRedisRepository currentLocationRedisRepository;
    @Mock private RouteDeviationService routeDeviationService;
    @Mock private StopDetectionService stopDetectionService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private GpsLocationRequest request(Long busId, double lat, double lon) {
        GpsLocationRequest req = new GpsLocationRequest();
        ReflectionTestUtils.setField(req, "busId", busId);
        ReflectionTestUtils.setField(req, "latitude", lat);
        ReflectionTestUtils.setField(req, "longitude", lon);
        ReflectionTestUtils.setField(req, "speed", 60.0);
        ReflectionTestUtils.setField(req, "heading", 90.0);
        return req;
    }

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

    @Test
    @DisplayName("GPS 위치 수신 성공 - DB 저장 및 WebSocket 브로드캐스트")
    void receiveLocation_success() {
        Bus bus = operatingBus(1L);
        GpsLocationRequest req = request(1L, 37.5665, 126.9780);

        given(busRepository.findById(1L)).willReturn(Optional.of(bus));
        BusLocation saved = BusLocation.builder()
                .id(1L).busId(1L).latitude(37.5665).longitude(126.9780)
                .speed(60.0).heading(90.0).recordedAt(LocalDateTime.now())
                .build();
        given(busLocationRepository.save(any(BusLocation.class))).willReturn(saved);
        willDoNothing().given(routeDeviationService).detect(any(Bus.class), any(GpsLocationRequest.class));
        willDoNothing().given(stopDetectionService).detect(any(GpsLocationRequest.class));

        gpsCommandService.receiveLocation(req);

        then(busLocationRepository).should().save(any(BusLocation.class));
        then(currentLocationRedisRepository).should().save(any());
        then(messagingTemplate).should().convertAndSend(eq("/topic/bus/1"), any(Object.class));
    }

    @Test
    @DisplayName("GPS 위치 수신 실패 - 버스 없음")
    void receiveLocation_busNotFound() {
        GpsLocationRequest req = request(99L, 37.5665, 126.9780);
        given(busRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> gpsCommandService.receiveLocation(req))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BUS_NOT_FOUND));
    }

    @Test
    @DisplayName("GPS 위치 수신 실패 - 버스 운행 중 아님")
    void receiveLocation_busNotOperating() {
        Bus bus = stoppedBus(1L);
        GpsLocationRequest req = request(1L, 37.5665, 126.9780);
        given(busRepository.findById(1L)).willReturn(Optional.of(bus));

        assertThatThrownBy(() -> gpsCommandService.receiveLocation(req))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BUS_NOT_OPERATING));
    }

    @Test
    @DisplayName("GPS 위치 수신 성공 - 경로 이탈 감지 서비스 호출됨")
    void receiveLocation_triggersRouteDeviation() {
        Bus bus = operatingBus(1L);
        GpsLocationRequest req = request(1L, 37.5665, 126.9780);
        BusLocation saved = BusLocation.builder()
                .id(1L).busId(1L).latitude(37.5665).longitude(126.9780)
                .speed(60.0).heading(90.0).recordedAt(LocalDateTime.now())
                .build();
        given(busRepository.findById(1L)).willReturn(Optional.of(bus));
        given(busLocationRepository.save(any(BusLocation.class))).willReturn(saved);

        gpsCommandService.receiveLocation(req);

        then(routeDeviationService).should().detect(eq(bus), eq(req));
        then(stopDetectionService).should().detect(eq(req));
    }
}
