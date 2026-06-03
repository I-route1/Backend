package com.i_route.backend.ai.dto;

import com.i_route.backend.ai.entity.StudyChecklistItem;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class StudyChecklistResponse {
    private Long id;
    private String studentId;
    private String subject;
    private String task;
    private int estimatedMinutes;
    private boolean done;
    private LocalDate planDate;

    public static StudyChecklistResponse from(StudyChecklistItem item) {
        return StudyChecklistResponse.builder()
                .id(item.getId())
                .studentId(item.getStudentId())
                .subject(item.getSubject())
                .task(item.getTask())
                .estimatedMinutes(item.getEstimatedMinutes())
                .done(item.isDone())
                .planDate(item.getPlanDate())
                .build();
    }
}
