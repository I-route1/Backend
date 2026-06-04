package com.i_route.backend.ai.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReviewTodayDto {

    private Long studentId;
    private boolean hasReview;
    private List<ReviewItem> reviews;

    @Getter
    @Builder
    public static class ReviewItem {
        private Long id;
        private String title;
        private String dayLabel;
        private String originalDate;
        private String message;
    }
}
