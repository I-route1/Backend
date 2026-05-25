package com.i_route.backend.gps.domain.gps.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EtaResponse {

    private Long studentId;

    private String studentName;

    private Long stopId;

    private String stopName;

    private Integer etaMinutes;

    private Integer delayMinutes;

    private Boolean lateRisk;

    private String delayReason;
}