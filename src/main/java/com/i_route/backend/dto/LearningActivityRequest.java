package com.i_route.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class LearningActivityRequest {
    private String studentId;
    private String subject;
    private LocalDate studyDate;
    private int studyDurationMinutes;
    private int understandingScore;
    private int concentrationScore;
    private String instructorFeedback;
}