package com.i_route.backend.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class GradeRequest {
    private Long studentId;
    private String subject;
    private int score;
    private int gradeLevel;
    private String examType;
    private LocalDate examDate;
}