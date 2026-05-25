package com.i_route.backend.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SingleCheckRequest {
    private String type;
    private String value;
}
