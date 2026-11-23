package com.i_route.backend.gps;

import com.i_route.backend.gps.domain.bus.entity.Bus;
import com.i_route.backend.gps.domain.bus.entity.OperationStatus;
import com.i_route.backend.gps.domain.bus.repository.BusRepository;
import com.i_route.backend.gps.domain.driver.entity.Driver;
import com.i_route.backend.gps.domain.gps.dto.response.CurrentBusLocationResponse;
import com.i_route.backend.gps.domain.gps.dto.response.EtaResponse;
import com.i_route.backend.gps.domain.gps.dto.response.RouteStopResponse;
import com.i_route.backend.gps.domain.gps.repository.CurrentLocationRedisRepository;
import com.i_route.backend.gps.domain.gps.service.EtaService;
import com.i_route.backend.gps.domain.gps.service.GpsQueryService;
import com.i_route.backend.gps.domain.gps.service.RoadPolylineService;
import com.i_route.backend.gps.domain.gps.service.RoadProjectionService;
import com.i_route.backend.gps.domain.route.entity.Route;
import com.i_route.backend.gps.domain.route.entity.RouteStop;
import com.i_route.backend.gps.domain.route.entity.StopStatus;
import com.i_route.backend.gps.domain.route.repository.RouteStopRepository;
import com.i_route.backend.gps.domain.route.service.RouteProgressService;
import com.i_route.backend.global.exception.CustomException;
import com.i_route.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class GpsQueryServiceTest {

    @InjectMocks
    private GpsQueryService gpsQueryService;

    @Mock private BusRepository busRepository;
    @Mock private RouteStopRepository routeStopRepository;
    @Mock private CurrentLocationRedisRepository currentLocationRedisRepository;
    private final RoadProjectionService projector = mock(RoadProjectionService.class);
    @Spy private RouteProgressService routeProgressService = new RouteProgressService(projector);
    @Mock private EtaService etaService;
    @Mock private RoadPolylineService roadPolylineService;

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

    @ParameterizedTest
    @EnumSource(value = OperationStatus.class, names = {"READY", "OPERATING"})
    void getBusRoute_withoutGpsReturnsPlannedStops(OperationStatus operationStatus) throws Exception {
        Route route = Route.builder().id(2L).routeName("수성 1호선").build();
        Driver driver = Driver.builder().id(3L).name("김민수").phoneNumber("010-1234-5678").build();
        Bus bus = Bus.builder().id(1L).route(route).driver(driver)
                .busNumber("대구 70바 1234").operationStatus(operationStatus).build();
        List<RouteStop> stops = List.of(
                RouteStop.builder().id(5L).routeId(2L).stopName("수성못역").stopOrder(1)
                        .latitude(35.831).longitude(128.623).scheduledArrivalTime(LocalTime.of(16, 30)).build(),
                RouteStop.builder().id(6L).routeId(2L).stopName("만촌역").stopOrder(2)
                        .latitude(35.858).longitude(128.644).build());
        given(busRepository.findById(1L)).willReturn(Optional.of(bus));
        given(currentLocationRedisRepository.findByBusId(1L)).willReturn(Optional.empty());
        given(routeStopRepository.findByRouteIdOrderByStopOrderAsc(2L)).willReturn(stops);
        given(roadPolylineService.getRoad(2L)).willReturn(List.of(
                new RoadPolylineService.Point(35.831, 128.623),
                new RoadPolylineService.Point(35.858, 128.644)));

        var result = gpsQueryService.getBusRoute(1L);

        assertThat(result.getBusId()).isEqualTo(1L);
        assertThat(result.getRouteName()).isEqualTo("수성 1호선");
        assertThat(result.getDriverName()).isEqualTo("김민수");
        assertThat(result.getDriverPhoneNumber()).isNull();
        assertThat(result.getStops()).extracting(RouteStopResponse::getStopId).containsExactly(5L, 6L);
        assertThat(result.getStops()).extracting(RouteStopResponse::getStatus)
                .containsExactly(StopStatus.BEFORE_ARRIVAL, StopStatus.BEFORE_ARRIVAL);
        assertThat(result.getStops()).extracting(RouteStopResponse::getEtaMinutes).containsOnlyNulls();
        assertThat(result.getStops()).extracting(RouteStopResponse::getScheduledArrivalTime)
                .containsExactly(LocalTime.of(16, 30), null);
        assertThat(result.getPath()).hasSize(2);
        String json = Jackson2ObjectMapperBuilder.json().build().writeValueAsString(result);
        assertThat(json).contains("\"scheduledArrivalTime\":\"16:30:00\"");
        then(projector).shouldHaveNoInteractions();
        then(etaService).shouldHaveNoInteractions();
    }

    @Test
    void getBusRoute_withGpsPreservesProgressAndScheduledTime() {
        Route route = Route.builder().id(2L).routeName("수성 1호선").build();
        Bus bus = Bus.builder().id(1L).route(route)
                .driver(Driver.builder().name("김민수").phoneNumber("010-1234-5678").build()).build();
        CurrentBusLocationResponse current = location(1L);
        RouteStop stop = RouteStop.builder().id(6L).routeId(2L).stopName("만촌역").stopOrder(1)
                .latitude(current.getLatitude()).longitude(current.getLongitude())
                .scheduledArrivalTime(LocalTime.of(17, 0)).build();
        given(busRepository.findById(1L)).willReturn(Optional.of(bus));
        given(currentLocationRedisRepository.findByBusId(1L)).willReturn(Optional.of(current));
        given(routeStopRepository.findByRouteIdOrderByStopOrderAsc(2L)).willReturn(List.of(stop));
        given(projector.project(2L, current.getLatitude(), current.getLongitude()))
                .willReturn(new RoadProjectionService.Projection(0, 0, 100));
        given(roadPolylineService.getRoad(2L)).willReturn(List.of());

        var result = gpsQueryService.getBusRoute(1L);

        assertThat(result.getDriverPhoneNumber()).isEqualTo("010-1234-5678");
        assertThat(result.getStops()).singleElement().satisfies(response -> {
            assertThat(response.getStatus()).isEqualTo(StopStatus.ARRIVED);
            assertThat(response.getScheduledArrivalTime()).isEqualTo(LocalTime.of(17, 0));
            assertThat(response.getEtaMinutes()).isNull();
        });
        then(routeProgressService).should().calculateStopStatuses(current.getLatitude(), current.getLongitude(), List.of(stop));
        then(etaService).shouldHaveNoInteractions();
    }

    private static Stream<RuntimeException> roadFailures() {
        return Stream.of(new IllegalStateException("Kakao configuration missing"),
                new CustomException(ErrorCode.KAKAO_ROUTE_NOT_FOUND),
                new CustomException(ErrorCode.KAKAO_API_ERROR),
                new RestClientException("Kakao request failed"));
    }

    @ParameterizedTest
    @MethodSource("roadFailures")
    void getBusRoute_withoutGpsStillReturnsScheduleWhenRoadIsUnavailable(RuntimeException error) {
        Route route = Route.builder().id(2L).routeName("수성 1호선").build();
        Bus bus = Bus.builder().id(1L).route(route)
                .driver(Driver.builder().name("김민수").phoneNumber("010-1234-5678").build()).build();
        RouteStop stop = RouteStop.builder().id(6L).routeId(2L).stopName("만촌역").stopOrder(1)
                .latitude(35.858).longitude(128.644).scheduledArrivalTime(LocalTime.of(17, 0)).build();
        given(busRepository.findById(1L)).willReturn(Optional.of(bus));
        given(currentLocationRedisRepository.findByBusId(1L)).willReturn(Optional.empty());
        given(routeStopRepository.findByRouteIdOrderByStopOrderAsc(2L)).willReturn(List.of(stop));
        given(roadPolylineService.getRoad(2L)).willThrow(error);

        var result = gpsQueryService.getBusRoute(1L);

        assertThat(result.getPath()).isEmpty();
        assertThat(result.getDriverPhoneNumber()).isNull();
        assertThat(result.getStops()).singleElement().satisfies(response -> {
            assertThat(response.getStopName()).isEqualTo("만촌역");
            assertThat(response.getStatus()).isEqualTo(StopStatus.BEFORE_ARRIVAL);
            assertThat(response.getScheduledArrivalTime()).isEqualTo(LocalTime.of(17, 0));
        });
        then(projector).shouldHaveNoInteractions();
        then(etaService).shouldHaveNoInteractions();
    }

    @Test
    void getBusRoute_withGpsStillReportsRoadFailures() {
        Route route = Route.builder().id(2L).routeName("수성 1호선").build();
        Bus bus = Bus.builder().id(1L).route(route).build();
        RuntimeException error = new CustomException(ErrorCode.KAKAO_API_ERROR);
        given(busRepository.findById(1L)).willReturn(Optional.of(bus));
        given(currentLocationRedisRepository.findByBusId(1L)).willReturn(Optional.of(location(1L)));
        given(routeStopRepository.findByRouteIdOrderByStopOrderAsc(2L)).willReturn(List.of());
        given(roadPolylineService.getRoad(2L)).willThrow(error);

        assertThatThrownBy(() -> gpsQueryService.getBusRoute(1L)).isSameAs(error);
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
