package com.i_route.backend.domain.gps.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class GpsLocationRequest {

    @NotNull
    private Long busId;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    @NotNull
    private Double speed;

    @NotNull
    private Double heading;
}
