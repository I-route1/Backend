package com.i_route.backend.ai.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.Map;

@Getter
@Builder
public class StudyPatternDto {
    private Long studentId;
    private String goldenTime;
    private Map<Long, Integer> subjectStudyMinutes;
    private String studyBalanceSummary;
}
