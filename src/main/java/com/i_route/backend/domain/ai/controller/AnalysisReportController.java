package com.i_route.backend.domain.ai.controller;

import com.i_route.backend.domain.ai.dto.AnalysisReportDto;
import com.i_route.backend.domain.ai.dto.MetaCognitionGapDto;
import com.i_route.backend.domain.ai.dto.PredictedScoreDto;
import com.i_route.backend.domain.ai.dto.StrengthAnalysisDto;
import com.i_route.backend.domain.ai.dto.StudyPatternDto;
import com.i_route.backend.domain.ai.service.PredictionService;
import com.i_route.backend.domain.ai.service.ReportAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisReportController {

    private final ReportAnalysisService reportAnalysisService;
    private final PredictionService predictionService;

    /**
     * GET /api/analysis/report?studentId=S-0155&subject=수학
     * (기존 String 기반 API 유지)
     */
    @GetMapping("/report")
    public Mono<ResponseEntity<AnalysisReportDto>> getLegacyScoreReport(
            @RequestParam String studentId,
            @RequestParam String subject) {
        return Mono.justOrEmpty(reportAnalysisService.getAnalysisReport(studentId, subject))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/analysis/score-report?studentId={id}&testId={id}
     * 특정 시험의 종합 백분위/비교 리포트 응답
     */
    @GetMapping("/score-report")
    public ResponseEntity<AnalysisReportDto> getScoreReport(
            @RequestParam Long studentId,
            @RequestParam Long testId) {
        return ResponseEntity.ok(reportAnalysisService.generateScoreReport(studentId, testId));
    }

    /**
     * GET /api/analysis/study-pattern?studentId={id}
     * 집중 시간대 및 과목 밸런스 분석 결과 응답
     */
    @GetMapping("/study-pattern")
    public ResponseEntity<StudyPatternDto> getStudyPattern(
            @RequestParam Long studentId) {
        return ResponseEntity.ok(reportAnalysisService.analyzeStudyPattern(studentId));
    }

    /**
     * GET /api/analysis/strengths?studentId={id}
     * 정답률이 높은 강점 단원 결과 응답
     */
    @GetMapping("/strengths")
    public ResponseEntity<StrengthAnalysisDto> getStrengthAnalysis(
            @RequestParam Long studentId) {
        return ResponseEntity.ok(reportAnalysisService.analyzeStrengths(studentId));
    }

    /**
     * GET /api/analysis/predict?studentId={id}&subjectId={id}
     * 다음 시험 예상 점수 구간 및 달성 확률 응답
     */
    @GetMapping("/predict")
    public ResponseEntity<PredictedScoreDto> predictScore(
            @RequestParam Long studentId,
            @RequestParam Long subjectId) {
        return ResponseEntity.ok(predictionService.predictNextScore(studentId, subjectId));
    }

    /**
     * GET /api/analysis/meta-cognition?studentId=S-0155&subject=수학
     * 자기평가 vs 실제 성적 괴리 분석 — 메타인지 역량 진단
     */
    @GetMapping("/meta-cognition")
    public ResponseEntity<MetaCognitionGapDto> getMetaCognitionGap(
            @RequestParam String studentId,
            @RequestParam String subject) {
        return ResponseEntity.ok(reportAnalysisService.analyzeMetaCognitionGap(studentId, subject));
    }
}