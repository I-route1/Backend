package com.i_route.backend.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class ParentRegisterRequestDto {

    private String username;

    private String nickname;

    private String password;

    private String passwordConfirm;

    private String name;

    private String email;

    private String phoneNumber;

    private String role;
}