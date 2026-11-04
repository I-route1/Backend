package com.i_route.backend.ai.dto;

import com.i_route.backend.ai.entity.LearningActivity;
import com.i_route.backend.ai.entity.MeasurementSource;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class LearningActivityResponse {
    private Long id;
    private String submissionId;
    private String subject;
    private LocalDate studyDate;
    private LocalTime studyStartTime;
    private int studyDurationMinutes;
    private int understandingScore;
    private int concentrationScore;
    private Double focusedRatio;
    private Double drowsyRatio;
    private MeasurementSource measurementSource;
    private String instructorFeedback;

    // 🌟 Entity를 DTO로 쉽게 변환해 주는 헬퍼 메서드
    public static LearningActivityResponse from(LearningActivity activity) {
        return LearningActivityResponse.builder()
                .id(activity.getId())
                .submissionId(activity.getSubmissionId())
                .subject(activity.getSubject())
                .studyDate(activity.getStudyDate())
                .studyStartTime(activity.getStudyStartTime())
                .studyDurationMinutes(activity.getStudyDurationMinutes())
                .understandingScore(activity.getUnderstandingScore())
                .concentrationScore(activity.getConcentrationScore())
                .focusedRatio(activity.getFocusedRatio())
                .drowsyRatio(activity.getDrowsyRatio())
                .measurementSource(activity.getMeasurementSource() == null
                        ? MeasurementSource.MANUAL : activity.getMeasurementSource())
                .instructorFeedback(activity.getInstructorFeedback())
                .build();
    }
}
