package com.i_route.backend.ai.controller;

import com.i_route.backend.ai.dto.AiReportResponse;
import com.i_route.backend.ai.service.AiCounselingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/counseling")
@RequiredArgsConstructor
public class MathAiController {

    private final AiCounselingService aiCounselingService;

    @PostMapping("/math")
    public ResponseEntity<AiReportResponse> generateMathReport(@RequestParam("studentId") String studentId) {
        AiReportResponse response = aiCounselingService.generateMathReport(studentId).block(Duration.ofSeconds(180));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/writing")
    public ResponseEntity<AiReportResponse> generateWritingReport(@RequestParam("studentId") String studentId) {
        AiReportResponse response = aiCounselingService.generateWritingReport(studentId).block(Duration.ofSeconds(180));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/premium")
    public ResponseEntity<AiReportResponse> generatePremiumReport(@RequestParam("studentId") String studentId) {
        AiReportResponse response = aiCounselingService.generatePremiumReport(studentId).block(Duration.ofSeconds(180));
        return ResponseEntity.ok(response);
    }
}