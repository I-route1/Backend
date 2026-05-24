package com.i_route.backend.ai.controller;

import com.i_route.backend.ai.dto.AiReportRequest;
import com.i_route.backend.ai.dto.AiReportResponse;
import com.i_route.backend.ai.service.AiCounselingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/counseling")
@RequiredArgsConstructor
public class MathAiController {

    private final AiCounselingService aiCounselingService;

    // 📐 1. 수학 메타인지 분석 리포트 API
    @PostMapping("/math")
    public Mono<AiReportResponse> generateMathReport(@RequestParam("studentId") String studentId) {
        return aiCounselingService.generateMathReport(studentId);
    }

    // ✍️ 2. 인공지능 기반 진로 탐색 리포트 API
    @PostMapping("/writing")
    public Mono<AiReportResponse> generateWritingReport(@RequestParam("studentId") String studentId) {
        return aiCounselingService.generateWritingReport(studentId);
    }

    // 🚀 3. i-Route 프리미엄 통합 리포트 API
    @PostMapping("/premium") // 👈 여기가 /premium 인지 확인!
    public Mono<AiReportResponse> generatePremiumReport(@RequestParam("studentId") String studentId) {
        return aiCounselingService.generatePremiumReport(studentId);
    }
}