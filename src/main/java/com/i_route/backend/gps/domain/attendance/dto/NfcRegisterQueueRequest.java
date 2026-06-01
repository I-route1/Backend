package com.i_route.backend.gps.domain.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class NfcRegisterQueueRequest {

    @NotNull
    private Long studentId;
}
