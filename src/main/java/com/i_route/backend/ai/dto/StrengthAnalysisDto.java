package com.i_route.backend.ai.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class StrengthAnalysisDto {
    private Long studentId;
    private List<Long> strongSubjectIds;
    private String strengthSummary;
}
