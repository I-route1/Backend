package com.i_route.backend.domain.gps.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CurrentBusLocationResponse {

    private Long busId;

    private Double latitude;

    private Double longitude;

    private Double speed;

    private Double heading;

    private LocalDateTime updatedAt;

    private String busNumber;

    private String driverName;

    private String driverPhoneNumber;
}
