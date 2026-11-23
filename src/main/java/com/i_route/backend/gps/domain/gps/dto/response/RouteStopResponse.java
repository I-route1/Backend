package com.i_route.backend.gps.domain.gps.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.i_route.backend.gps.domain.route.entity.StopStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@Builder
public class RouteStopResponse {

    private Long stopId;

    private String stopName;

    private Double latitude;

    private Double longitude;

    private Integer stopOrder;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime scheduledArrivalTime;

    private StopStatus status;

    private Integer etaMinutes;
}
