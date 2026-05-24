package com.i_route.backend.ai.controller;

import com.i_route.backend.ai.dto.ReviewPaperDto;
import com.i_route.backend.ai.dto.StudyRoadmapDto;
import com.i_route.backend.ai.dto.TargetGoalDto;
import com.i_route.backend.ai.service.ReviewGenerationService;
import com.i_route.backend.ai.service.StudyPlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/study-plan")
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanningService studyPlanningService;
    private final ReviewGenerationService reviewGenerationService;

    /**
     * POST /api/study-plan/review-paper?studentId={id}
     * 취약 개념 기반의 맞춤형 복습 문제지 생성 응답
     */
    @PostMapping("/review-paper")
    public ResponseEntity<ReviewPaperDto> generateReviewPaper(
            @RequestParam Long studentId) {
        return ResponseEntity.ok(reviewGenerationService.generateCustomReviewPaper(studentId));
    }

    /**
     * POST /api/study-plan/progress?studentId={id}
     * 목표일에 맞춘 일일 최적 학습 분량 배분 로드맵 응답
     */
    @PostMapping("/progress")
    public ResponseEntity<StudyRoadmapDto> designProgressPlan(
            @RequestParam Long studentId,
            @RequestBody TargetGoalDto goalDto) {
        return ResponseEntity.ok(studyPlanningService.designProgressPlan(studentId, goalDto));
    }
}
