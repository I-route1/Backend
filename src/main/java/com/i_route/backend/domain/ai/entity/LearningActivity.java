package com.i_route.backend.domain.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LearningActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentId;           // 학생 ID

    private String subject;             // 학습 과목 (예: 국어, 수학, 영어)

    private LocalDate studyDate;        // 학습 진행 일자

    private int studyDurationMinutes;   // 앱 접속/학습 지속 시간 (분 단위)

    // 🌟 메타인지 영역 (학생 자기 평가)
    private int understandingScore;     // 주관적 이해도 별점 (1~5점)

    private int concentrationScore;     // 주관적 집중도 별점 (1~5점)

    // 🌟 강사 피드백 영역
    @Column(length = 1000)              // 피드백은 길어질 수 있으므로 글자 수 넉넉히 확보
    private String instructorFeedback;  // 담당 강사의 정성적 평가 및 코멘트
}