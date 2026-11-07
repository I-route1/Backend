package com.i_route.backend.ai.dto;

import com.i_route.backend.ai.entity.AiRecommendation;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record CounselingReportResponse(Long id, String title, String subject,
                                       String careerAnalysis, String learningGuide, LocalDateTime createdAt) {
    public static CounselingReportResponse from(AiRecommendation report) {
        String subject = report.getSubject();
        if (subject == null || subject.isBlank()) subject = legacySubject(report.getTitle());
        return new CounselingReportResponse(report.getId(), report.getTitle(), subject,
                report.getCareerAnalysis(), report.getLearningGuide(), report.getCreatedAt());
    }

    // 기존 생성기가 사용한 제목만 해석한다. 알 수 없는 과목을 추측하지 않는다.
    private static String legacySubject(String title) {
        if (title == null) return null;
        return switch (title) {
            case "수학 메타인지 분석 리포트" -> "수학";
            case "인공지능 기반 진로 탐색 리포트" -> "국어";
            case "영어 메타인지 분석 리포트" -> "영어";
            case "과학 메타인지 분석 리포트" -> "과학";
            case "사회 메타인지 분석 리포트" -> "사회";
            case "i-Route 프리미엄 통합 리포트" -> "통합";
            default -> null;
        };
    }
}
