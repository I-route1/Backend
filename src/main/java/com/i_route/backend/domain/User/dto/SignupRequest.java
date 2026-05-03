package com.i_route.backend.domain.user.dto;

import lombok.Getter;

@Getter
public class SignupRequest {
    private String name;
    private String email;
    private String password;
}