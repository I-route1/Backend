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
    public AiReportResponse generateMathReport(@RequestParam("studentId") Long studentId) {
        return aiCounselingService.generateMathReport(studentId).block();
    }

    @PostMapping("/writing")
    public AiReportResponse generateWritingReport(@RequestParam("studentId") Long studentId) {
        return aiCounselingService.generateWritingReport(studentId).block();
    }

    @PostMapping("/premium")
    public AiReportResponse generatePremiumReport(@RequestParam("studentId") Long studentId) {
        return aiCounselingService.generatePremiumReport(studentId).block();
    }

    /**
     * 영어/과학/사회/한국사 메타인지 분석 리포트.
     * 수학·국어·프리미엄은 위의 고정 경로가 먼저 매칭되므로 여기로 들어오지 않는다.
     */
    @PostMapping("/{subject}")
    public AiReportResponse generateSubjectReport(@PathVariable("subject") String subject,
                                                  @RequestParam("studentId") Long studentId) {
        return aiCounselingService.generateSubjectReport(studentId, subject).block();
    }
}