package com.i_route.backend.controller;

import com.i_route.backend.dto.MaterialRecommendationDto;
import com.i_route.backend.dto.StudyRoadmapDto;
import com.i_route.backend.entity.WrongAnswerEntity;
import com.i_route.backend.service.AiRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final AiRecommendationService aiRecommendationService;

    /**
     * GET /api/recommendations/materials?studentId={id}
     * 학생 수준에 맞는 최적화된 자료 리스트 응답
     */
    @GetMapping("/materials")
    public ResponseEntity<List<MaterialRecommendationDto>> getMaterialRecommendations(
            @RequestParam Long studentId) {
        return ResponseEntity.ok(aiRecommendationService.recommendMaterials(studentId));
    }

    /**
     * GET /api/recommendations/roadmap?studentId={id}
     * 목표 기반 단계별 학습 커리큘럼 로드맵 응답
     */
    @GetMapping("/roadmap")
    public ResponseEntity<StudyRoadmapDto> getGoalRoadmap(
            @RequestParam Long studentId) {
        return ResponseEntity.ok(aiRecommendationService.recommendGoalRoadmap(studentId));
    }

    /**
     * GET /api/recommendations/daily-review?studentId={id}
     * 에빙하우스 기준 당일 최우선 복습 대상 목록 응답
     */
    @GetMapping("/daily-review")
    public ResponseEntity<List<WrongAnswerEntity>> getDailyReviewTasks(
            @RequestParam Long studentId) {
        return ResponseEntity.ok(aiRecommendationService.recommendDailyReview(studentId));
    }
}
