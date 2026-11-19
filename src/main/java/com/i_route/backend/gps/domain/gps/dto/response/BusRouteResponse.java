
package com.i_route.backend.gps.domain.gps.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class BusRouteResponse {

    private Long busId;

    private Long routeId;

    private String routeName;

    private String busNumber;

    private String driverName;

    private String driverPhoneNumber;

    private List<RouteStopResponse> stops;

    // 실제 도로 경로 좌표
    private List<RoutePathPoint> path;

    public record RoutePathPoint(
            double latitude,
            double longitude
    ) {}
}

