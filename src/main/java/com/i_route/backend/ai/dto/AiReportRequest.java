package com.i_route.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReportRequest {
    private Long studentId;
    private Double currentKoreanGrade;
    private Double studyTime;
    private String studentNote;
    // 수준 라벨("기초 강화 필요" 등)이지 개념 이름이 아니다. 취약 개념은 weakConcept로 넘긴다.
    private String recommendContext;
    // 리포트 과목에서 가장 많이 틀린 오답의 conceptTag. 오답이 없으면 빈 문자열이고,
    // null이면(과목이 정해지지 않는 프리미엄 리포트) AI 서버가 오답 API로 직접 찾는다.
    private String weakConcept;
    private String instructorFeedback;
}