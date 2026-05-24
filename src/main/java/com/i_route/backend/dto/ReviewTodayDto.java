package com.i_route.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReviewTodayDto {

    private String studentId;
    private boolean hasReview;
    private List<ReviewItem> reviews;

    @Getter
    @Builder
    public static class ReviewItem {
        private String title;
        private String dayLabel;
        private String originalDate;
        private String message;
    }
}
