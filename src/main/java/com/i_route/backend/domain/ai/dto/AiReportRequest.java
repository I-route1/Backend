package com.i_route.backend.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReportRequest {
    private String studentId;
    private Double currentKoreanGrade;
    private Double studyTime;
    private String studentNote;
    private String recommendContext;
}