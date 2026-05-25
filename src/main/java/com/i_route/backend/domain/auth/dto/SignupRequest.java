package com.i_route.backend.domain.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    private Long id;

    private String nickname;

    private String password;

    private String email;

    private String phoneNumber;

    private String role;
}