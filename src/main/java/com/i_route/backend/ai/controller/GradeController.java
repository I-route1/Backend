package com.i_route.backend.ai.controller;

import com.i_route.backend.ai.dto.GradeRequest;
import com.i_route.backend.ai.dto.GradeResponse;
import com.i_route.backend.ai.service.GradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.i_route.backend.ai.dto.GradeAnalysisResponse;
import java.util.List;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    // 🌟 1. 성적 입력 API (POST)
    @PostMapping
    public ResponseEntity<GradeResponse> saveGrade(@RequestBody GradeRequest request) {
        GradeResponse response = gradeService.saveGrade(request);
        return ResponseEntity.ok(response);
    }

    // 🌟 2. 성적 조회 API (GET)
    @GetMapping("/{studentId}")
    public ResponseEntity<List<GradeResponse>> getGrades(@PathVariable String studentId) {
        List<GradeResponse> responses = gradeService.getGradesByStudent(studentId);
        return ResponseEntity.ok(responses);
    }

    // 🌟 [NEW] 3. 학생 성적 추이 및 요약 분석 리포트 API (GET)
    @GetMapping("/{studentId}/analysis")
    public ResponseEntity<GradeAnalysisResponse> getGradeAnalysis(@PathVariable String studentId) {
        GradeAnalysisResponse response = gradeService.analyzeStudentGrades(studentId);
        return ResponseEntity.ok(response);
    }
}