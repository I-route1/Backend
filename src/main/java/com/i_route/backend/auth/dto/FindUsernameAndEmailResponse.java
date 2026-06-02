package com.i_route.backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FindUsernameAndEmailResponse {
    private String username;
    private String email;
}