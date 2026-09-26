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
    // 프리미엄은 weakSubject 과목의 개념이다.
    private String weakConcept;
    // 프리미엄 리포트만: 오답 failCount 합이 가장 큰 과목(weakConcept도 이 과목 것). 오답이 없으면 null.
    private String weakSubject;
    // 리포트 과목(프리미엄은 weakSubject) 가장 최근 시험의 백분위. 없으면 null(AI 서버가 국어 백분위로 대신한다).
    private Double subjectPercentile;
    private String instructorFeedback;
}