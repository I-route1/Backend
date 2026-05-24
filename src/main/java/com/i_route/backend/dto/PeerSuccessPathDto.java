package com.i_route.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PeerSuccessPathDto {
    private String subject;
    private int similarStudentsCount;
    private double avgInitialScore;
    private double avgFinalScore;
    private double avgImprovement;
    private String studyStrategyPattern;
    private String successMessage;
    private List<String> keyInsights;
}
