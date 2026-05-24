package com.i_route.backend.domain.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SingleCheckRequest {
    private String type;
    private String value;
}
