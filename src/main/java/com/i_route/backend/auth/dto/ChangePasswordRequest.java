package com.i_route.backend.auth.dto;

import lombok.*;

@Getter
@Setter
public class ChangePasswordRequest {

    private Long userId;

    private String currentPassword;

    private String newPassword;

    private String newPasswordConfirm;
}