package com.i_route.backend.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserProfileUpdateRequest {
    private String name;
    private String nickname;
    private String email;
    private String phoneNumber;
}