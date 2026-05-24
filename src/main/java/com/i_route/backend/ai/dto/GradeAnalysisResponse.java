package com.i_route.backend.ai.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class GradeAnalysisResponse {
    private String studentId;

    // 📈 시계열 그래프용 전체 성적 이력 (날짜 오름차순 정렬)
    private List<GradeResponse> gradeHistory;

    // 📊 과목별 가장 최근 점수 변동 폭 (ex: "국어": +5, "수학": -3)
    private Map<String, Integer> subjectScoreChanges;

    // 📝 요약 리포트용 문구 (ex: "전월 대비 국어 성적이 5점 상승했습니다!")
    private String summaryMessage;
}