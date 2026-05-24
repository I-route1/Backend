package com.i_route.backend.gps.domain.gps.dto.response;

import com.i_route.backend.gps.domain.route.entity.StopStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RouteStopResponse {

    private Long stopId;

    private String stopName;

    private Double latitude;

    private Double longitude;

    private Integer stopOrder;

    private StopStatus status;

    private Integer etaMinutes;
}
