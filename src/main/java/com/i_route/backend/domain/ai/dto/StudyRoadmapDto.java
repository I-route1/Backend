package com.i_route.backend.domain.ai.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class StudyRoadmapDto {
    private Long studentId;
    private String targetKeyword;
    private LocalDate targetDate;
    private List<String> weeklyMilestones;
    private String overallStrategy;
}
