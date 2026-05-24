package com.i_route.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MaterialRecommendationDto {
    private Long materialId;
    private String title;
    private String materialType;
    private String matchReason;
}
