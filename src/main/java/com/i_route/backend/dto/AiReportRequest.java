package com.i_route.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiReportRequest {
    private String studentId;
    private double currentKoreanGrade;
    private double studyHours;
    private String studentNote;
    private String teacherFeedback;
}