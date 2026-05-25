package com.i_route.backend.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SocialRegisterRequest {

    private String providerId;
    private String email;
    private String nickname;
    private String profileImage;
}
