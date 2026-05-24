package com.i_route.backend.controller;

import com.i_route.backend.dto.ReviewTodayDto;
import com.i_route.backend.service.ReviewSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewSchedulerService reviewSchedulerService;

    @GetMapping("/today")
    public ResponseEntity<ReviewTodayDto> getTodayReviews(@RequestParam String studentId) {
        return ResponseEntity.ok(reviewSchedulerService.getTodayReviews(studentId));
    }
}
