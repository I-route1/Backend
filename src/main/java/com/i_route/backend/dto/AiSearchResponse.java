package com.i_route.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class AiSearchResponse {
    private List<String> contexts;
}