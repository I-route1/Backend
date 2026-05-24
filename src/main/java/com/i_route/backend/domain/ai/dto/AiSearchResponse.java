package com.i_route.backend.domain.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class AiSearchResponse {
    private List<String> contexts;
}