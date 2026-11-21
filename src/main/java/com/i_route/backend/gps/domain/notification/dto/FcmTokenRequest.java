package com.i_route.backend.gps.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenRequest(
        @NotBlank String token
) {
}
