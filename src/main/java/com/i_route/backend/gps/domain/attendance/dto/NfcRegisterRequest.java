package com.i_route.backend.gps.domain.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class NfcRegisterRequest {

    @NotBlank
    private String nfcCardId;
}
