package com.i_route.backend.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AiReportResponse {
    private String studentId;
    private String title;
    private String careerAnalysis;  
    private String learningGuide;
}