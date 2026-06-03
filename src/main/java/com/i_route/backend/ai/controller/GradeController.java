package com.i_route.backend.ai.controller;

import com.i_route.backend.ai.dto.GradeRequest;
import com.i_route.backend.ai.dto.GradeResponse;
import com.i_route.backend.ai.dto.GradeUpdateRequest;
import com.i_route.backend.ai.service.GradeService;
import com.i_route.backend.gps.domain.attendance.dto.GradeStudentIdRequest;
import jakarta.validation.Valid;
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

    // 3. 성적 수정 API (PATCH)
    @PatchMapping("/{id}")
    public ResponseEntity<GradeResponse> updateGrade(
            @PathVariable Long id,
            @RequestBody GradeUpdateRequest request) {
        return ResponseEntity.ok(gradeService.updateGrade(id, request));
    }

    // 4. 성적 삭제 API (DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGrade(@PathVariable Long id) {
        gradeService.deleteGrade(id);
        return ResponseEntity.noContent().build();
    }

    // 🌟 [NEW] 5. 학생 성적 추이 및 요약 분석 리포트 API (GET)
    @GetMapping("/{studentId}/analysis")
    public ResponseEntity<GradeAnalysisResponse> getGradeAnalysis(@PathVariable String studentId) {
        GradeAnalysisResponse response = gradeService.analyzeStudentGrades(studentId);
        return ResponseEntity.ok(response);
    }

    // 6. 학생 성적 시스템 ID 등록 (PATCH)
    @PatchMapping("/students/{studentId}/grade-id")
    public ResponseEntity<Void> registerGradeStudentId(
            @PathVariable Long studentId,
            @Valid @RequestBody GradeStudentIdRequest request) {
        gradeService.registerGradeStudentId(studentId, request.getGradeStudentId());
        return ResponseEntity.ok().build();
    }
}