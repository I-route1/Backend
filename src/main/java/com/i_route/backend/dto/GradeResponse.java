package com.i_route.backend.dto;

import com.i_route.backend.entity.Grade;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
public class GradeResponse {
    private Long id;
    private String subject;
    private int score;
    private int gradeLevel;
    private String examType;
    private LocalDate examDate;

    public static GradeResponse from(Grade grade) {
        return GradeResponse.builder()
                .id(grade.getId())
                .subject(grade.getSubject())
                .score(grade.getScore())
                .gradeLevel(grade.getGradeLevel())
                .examType(grade.getExamType())
                .examDate(grade.getExamDate())
                .build();
    }
}