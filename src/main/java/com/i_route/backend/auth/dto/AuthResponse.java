package com.i_route.backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String userId;
    private String nickname;
    private String role;
    private Boolean isNewUser; // 소셜 로그인 신규 유저 여부
}
