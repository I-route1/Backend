package com.i_route.backend.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class StudyChecklistRequest {
    private Long studentId;
    private String subject;
    private String task;
    private int estimatedMinutes;
    private LocalDate planDate;
}
