package com.i_route.backend.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId; // 학생 ID

    private String subject;   // 과목명 (예: 국어, 수학, 영어)

    private int score;        // 원점수

    private int gradeLevel;   // 등급 (1~9등급)

    private double percentile; // 🚀 추가: 백분위 (전체 평균 대비 위치 분석 및 AI 예측용)

    private String examType;  // 시험 종류 (예: 중간고사, 기말고사, 3월모의고사)

    private LocalDate examDate; // 시행 일자

    private String weakConceptTag; // 🚀 추가: 오답 기반 취약 개념 태그 (RAG 검색 및 복습 추천용)

    // 🚀 추가: 기획 명세서에 정의된 비즈니스 편의 메서드 (엔티티 내부에서 안전하게 값 변경)
    public void updateScore(int score, int gradeLevel, double percentile) {
        this.score = score;
        this.gradeLevel = gradeLevel;
        this.percentile = percentile;
    }
}