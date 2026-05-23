package com.i_route.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PredictedScoreDto {
    private Integer expectedScoreMin;
    private Integer expectedScoreMax;
    private Double achievementProbability;
}
