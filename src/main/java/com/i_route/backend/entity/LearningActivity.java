package com.i_route.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LearningActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentId;

    // 학생 메타인지 (자기 평가)
    private int understandingScore; // 이해도 (1~5점)
    private int focusScore;         // 집중도 (1~5점)
    @Column(columnDefinition = "TEXT")
    private String studentNote;     // 학생이 직접 쓴 소감

    // 강사 정성 평가
    @Column(columnDefinition = "TEXT")
    private String teacherFeedback; // 담당 강사가 쓴 태도 및 특이사항
    private String teacherName;

    private LocalDateTime createdAt;

    @Builder
    public LearningActivity(String studentId, int understandingScore, int focusScore,
                            String studentNote, String teacherFeedback, String teacherName) {
        this.studentId = studentId;
        this.understandingScore = understandingScore;
        this.focusScore = focusScore;
        this.studentNote = studentNote;
        this.teacherFeedback = teacherFeedback;
        this.teacherName = teacherName;
        this.createdAt = LocalDateTime.now();
    }
}