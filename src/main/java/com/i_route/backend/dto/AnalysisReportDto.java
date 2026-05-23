// 💡 AnalysisReportDto.java (컨트롤러 반환용 데이터 규격)
package com.i_route.backend.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisReportDto {
    private String subjectName;
    private double myPercentile;
    private double averageScore;       // 전체 평균 점수
    private String weakPointSummary;   // 취약점 요약 텍스트
}