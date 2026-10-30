package com.i_route.backend.user.dto;

import com.i_route.backend.user.entity.User;
import lombok.Getter;

@Getter
public class UserResponseDto {
    private Long id;
    private String name;
    private String nickname;
    private String email;
    private String phoneNumber; // 👈 이 필드가 전달되어야 프론트에서 표시됩니다.
    private String role;

    public UserResponseDto(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.nickname = user.getNickname();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
        this.role = user.getRole().name();
    }
}