package com.i_route.backend.ai.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MetaCognitionGapDto {
    private String studentId;
    private String subject;
    private double avgUnderstandingScore;   // 학생 자기평가 평균 (1~5)
    private double avgActualScore;          // 실제 시험 점수 평균
    private double scaledUnderstandingScore; // 자기평가 × 20 (100점 환산)
    private double gapScore;               // |환산값 - 실제점수|
    private String gapLevel;               // HIGH / MEDIUM / LOW
    private String gapSummary;
    private String advice;
}
