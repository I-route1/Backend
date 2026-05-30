package com.i_route.backend.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AcademyRegisterRequestDto {

    // 회원 기본 정보
    private String username;

    private String nickname;

    private String password;

    private String passwordConfirm;

    private String name;

    private String email;

    private String phoneNumber;

    // 사용자 역할
    private String role;

    // 학원 정보
    private String academyName;

    private String academyAddress;

    private String businessNumber;
}