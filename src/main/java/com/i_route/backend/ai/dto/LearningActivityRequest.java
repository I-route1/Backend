package com.i_route.backend.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class LearningActivityRequest {
    private String studentId;
    private String subject;
    private LocalDate studyDate;
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime studyStartTime;
    private int studyDurationMinutes;
    private int understandingScore;
    private int concentrationScore;
    private String instructorFeedback;
}