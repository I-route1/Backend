package com.i_route.backend.gps.domain.student.entity;

import jakarta.persistence.*;
import lombok.*;


import java.time.LocalTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //@Column(name = "id")
    private Long studentId;

    private Long busId;

    private Long routeStopId;

    private String name;

    private LocalTime expectedDropOffTime;

    private Long parentId;

    private Long academyId;

    private String nfcCardId;

    private String gradeStudentId;

    private String grade;

    private String profileImage;

    // 학습/AI 관련 필드
    private Integer currentLevel;

    @Enumerated(EnumType.STRING)
    private LearningTendency learningTendency;

    private Double currentKoreanGrade;

    private Double studyTime;

    private String studentNote;

    private String recommendContext;

    public enum LearningTendency {
        VISUAL, AUDITORY, KINESTHETIC
    }
}
