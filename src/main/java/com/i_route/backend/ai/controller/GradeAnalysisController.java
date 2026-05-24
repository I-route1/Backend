package com.i_route.backend.ai.controller;

import com.i_route.backend.ai.service.GradeAnalysisService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeAnalysisController {

    private final GradeAnalysisService gradeAnalysisService;

    /**
     * 프론트엔드(앱/웹)에서 성적 데이터를 보내면 호출되는 API 엔드포인트입니다.
     * POST /api/grades/analyze
     */
    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeGrade(@RequestBody GradeInputRequest request) {

        // 1. 프론트엔드에서 넘어온 데이터를 방금 만든 Service로 넘겨서 분석 및 AI 호출 실행
        gradeAnalysisService.processStudentGrade(
                request.getScore(),
                request.getAllScores(),
                request.getWeakConceptTag()
        );

        // 2. 비동기로 AI가 족보를 찾는 동안, 프론트엔드에게는 "접수 완료" 응답을 먼저 빠르게 줍니다.
        return ResponseEntity.ok("성적 분석 및 AI 맞춤 족보 탐색이 성공적으로 시작되었습니다.");
    }

    /**
     * 프론트엔드가 백엔드로 성적을 보낼 때 사용할 입력 상자(내부 DTO)입니다.
     * 필요에 따라 dto 폴더로 따로 빼셔도 좋습니다.
     */
    @Data
    public static class GradeInputRequest {
        private int score;                // 이번 시험 내 점수
        private List<Integer> allScores;  // 전체 학생 점수 리스트 (평균/표준편차 계산용)
        private String weakConceptTag;    // 학생이 틀린 주요 개념 태그 (예: "이차방정식의 근과 계수")
    }
}