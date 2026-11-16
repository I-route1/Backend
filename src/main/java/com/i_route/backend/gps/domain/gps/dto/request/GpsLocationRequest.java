package com.i_route.backend.gps.domain.gps.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class GpsLocationRequest {

    @NotNull
    private Long busId;

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitude;

    // 브라우저 GPS 원본 단위: m/s
    @NotNull
    @DecimalMin("0.0")
    private Double speed;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("360.0")
    private Double heading;
}
