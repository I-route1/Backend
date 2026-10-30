package com.i_route.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String name;
    private String nickname;
    private String email;
    private String phoneNumber;
    private String role;
}