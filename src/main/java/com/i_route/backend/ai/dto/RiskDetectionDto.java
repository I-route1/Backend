package com.i_route.backend.ai.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class RiskDetectionDto {
    private String studentId;
    private boolean atRisk;
    private String riskLevel;       // NONE / LOW / HIGH
    private String riskSummary;
    private List<SubjectRisk> subjectRisks;

    @Getter
    @Builder
    public static class SubjectRisk {
        private String subject;
        private int consecutiveDrops;
        private double dropAmount;
        private String message;
    }
}
