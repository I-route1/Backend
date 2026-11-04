package com.i_route.backend.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_activity_student_submission",
        columnNames = {"student_id", "submission_id"}))
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LearningActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id")
    private Long studentId;           // 학생 ID

    @Column(name = "submission_id", length = 64)
    private String submissionId;

    private String subject;             // 학습 과목 (예: 국어, 수학, 영어)

    private LocalDate studyDate;        // 학습 진행 일자

    private LocalTime studyStartTime;   // 학습 시작 시각 (골든타임 분석용)

    private int studyDurationMinutes;   // 앱 접속/학습 지속 시간 (분 단위)

    // 🌟 메타인지 영역 (학생 자기 평가)
    private int understandingScore;     // 주관적 이해도 별점 (1~5점)

    private int concentrationScore;     // 주관적 집중도 별점 (1~5점)

    // 얼굴이 검출되어 분류된 표본 중 각 상태의 비율(0~1). 직접 입력은 null 허용.
    private Double focusedRatio;

    private Double drowsyRatio;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private MeasurementSource measurementSource;

    // 🌟 강사 피드백 영역
    @Column(length = 1000)
    private String instructorFeedback;

    public void updateFeedback(String feedback) {
        this.instructorFeedback = feedback;
    }

    public void attachSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }
}
