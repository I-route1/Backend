package com.i_route.backend.ai.dto;

import com.i_route.backend.ai.entity.LearningActivity;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
public class LearningActivityResponse {
    private Long id;
    private String subject;
    private LocalDate studyDate;
    private int studyDurationMinutes;
    private int understandingScore;
    private int concentrationScore;
    private String instructorFeedback;

    // 🌟 Entity를 DTO로 쉽게 변환해 주는 헬퍼 메서드
    public static LearningActivityResponse from(LearningActivity activity) {
        return LearningActivityResponse.builder()
                .id(activity.getId())
                .subject(activity.getSubject())
                .studyDate(activity.getStudyDate())
                .studyDurationMinutes(activity.getStudyDurationMinutes())
                .understandingScore(activity.getUnderstandingScore())
                .concentrationScore(activity.getConcentrationScore())
                .instructorFeedback(activity.getInstructorFeedback())
                .build();
    }
}