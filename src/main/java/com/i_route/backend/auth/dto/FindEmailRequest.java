package com.i_route.backend.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FindEmailRequest {
    private String phoneNumber;
}