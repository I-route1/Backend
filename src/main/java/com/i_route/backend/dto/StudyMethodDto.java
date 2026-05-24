package com.i_route.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudyMethodDto {
    private Long studentId;
    private Long subjectId;
    private String tendency;
    private String studyGuide;
    private String recommendedApproach;
}
