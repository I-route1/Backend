package com.i_route.backend.gps.domain.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class AttendanceTagRequest {

    @NotNull
    private Long busId;

    @NotBlank
    private String nfcCardId;
}
