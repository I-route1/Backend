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
}
