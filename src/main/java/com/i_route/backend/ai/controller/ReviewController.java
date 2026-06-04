package com.i_route.backend.ai.controller;

import com.i_route.backend.ai.dto.ReviewTodayDto;
import com.i_route.backend.ai.service.ReviewSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewSchedulerService reviewSchedulerService;

    @GetMapping("/today")
    public ResponseEntity<ReviewTodayDto> getTodayReviews(@RequestParam String studentId) {
        return ResponseEntity.ok(reviewSchedulerService.getTodayReviews(studentId));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Map<String, Boolean>> deleteReview(@PathVariable Long reviewId) {
        reviewSchedulerService.deleteReview(reviewId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
