package com.i_route.backend.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class GradeUpdateRequest {
    private String subject;
    private Integer score;
    private Integer gradeLevel;
    private String examType;
    private LocalDate examDate;
}
