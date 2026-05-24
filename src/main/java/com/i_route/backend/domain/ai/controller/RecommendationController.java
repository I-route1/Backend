package com.i_route.backend.domain.ai.controller;

import com.i_route.backend.domain.ai.dto.MaterialRecommendationDto;
import com.i_route.backend.domain.ai.dto.PeerSuccessPathDto;
import com.i_route.backend.domain.ai.dto.StudyMethodDto;
import com.i_route.backend.domain.ai.dto.StudyRoadmapDto;
import com.i_route.backend.domain.ai.entity.WrongAnswerEntity;
import com.i_route.backend.domain.ai.service.AiRecommendationService;
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

    /**
     * GET /api/recommendations/study-method?studentId={id}&subjectId={id}
     * 학습 성향(VISUAL/AUDITORY/KINESTHETIC) 기반 맞춤 공부법 제안
     */
    @GetMapping("/study-method")
    public ResponseEntity<StudyMethodDto> getStudyMethod(
            @RequestParam Long studentId,
            @RequestParam Long subjectId) {
        return ResponseEntity.ok(aiRecommendationService.suggestStudyMethod(studentId, subjectId));
    }

    /**
     * GET /api/recommendations/peer-content?studentId={id}
     * 유사 성적대 학생들이 선호하는 고효율 콘텐츠 매칭
     */
    @GetMapping("/peer-content")
    public ResponseEntity<List<MaterialRecommendationDto>> getPeerContent(
            @RequestParam String studentId) {
        return ResponseEntity.ok(aiRecommendationService.recommendByPeerContent(studentId));
    }

    /**
     * GET /api/recommendations/peer-path?studentId={id}&subject={subject}
     * 동일 목표를 달성한 선배 학생들의 성공 학습 경로 추천
     */
    @GetMapping("/peer-path")
    public ResponseEntity<PeerSuccessPathDto> getPeerSuccessPath(
            @RequestParam String studentId,
            @RequestParam String subject) {
        return ResponseEntity.ok(aiRecommendationService.recommendPeerSuccessPath(studentId, subject));
    }
}
