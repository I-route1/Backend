package com.i_route.backend.ai.controller;

import com.i_route.backend.ai.dto.AiReportResponse;
import com.i_route.backend.ai.service.AiCounselingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/counseling")
@RequiredArgsConstructor
public class MathAiController {

    private final AiCounselingService aiCounselingService;

    @PostMapping("/math")
    public AiReportResponse generateMathReport(@RequestParam("studentId") String studentId) {
        return aiCounselingService.generateMathReport(studentId).block();
    }

    @PostMapping("/writing")
    public AiReportResponse generateWritingReport(@RequestParam("studentId") String studentId) {
        return aiCounselingService.generateWritingReport(studentId).block();
    }

    @PostMapping("/premium")
    public AiReportResponse generatePremiumReport(@RequestParam("studentId") String studentId) {
        return aiCounselingService.generatePremiumReport(studentId).block();
    }
}