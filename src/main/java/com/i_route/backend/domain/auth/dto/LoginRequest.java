package com.i_route.backend.domain.auth.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor; // 🌟 추가

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    private String email;
    private String password;
}