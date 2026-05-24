// 💡 AnalysisReportDto.java (컨트롤러 반환용 데이터 규격)
package com.i_route.backend.ai.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisReportDto {
    private String subjectName;
    private double myPercentile;
    private double averageScore;
    private double standardDeviation;       // 표준편차 (학업 역량 객관화)
    private double scoreChangeFromPrevious; // 전회 시험 대비 점수 변동
    private String trendSummary;            // "상승" / "하락" / "유지"
    private String weakPointSummary;
}