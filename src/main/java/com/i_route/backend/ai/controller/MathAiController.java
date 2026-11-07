package com.i_route.backend.ai.controller;

import com.i_route.backend.ai.dto.AiReportResponse;
import com.i_route.backend.ai.dto.CounselingReportResponse;
import com.i_route.backend.ai.service.AiCounselingService;
import com.i_route.backend.ai.service.StudentRecordAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/counseling")
@RequiredArgsConstructor
public class MathAiController {

    private final AiCounselingService aiCounselingService;
    private final StudentRecordAccess recordAccess;

    @GetMapping("/reports")
    public List<CounselingReportResponse> getReports(@RequestParam("studentId") Long studentId,
                                                      HttpServletRequest request) {
        String[] ids = request.getParameterValues("studentId");
        if (ids == null || ids.length != 1 || !ids[0].matches("[0-9]+")) {
            throw new IllegalArgumentException("studentId는 양의 십진수 정수 하나여야 합니다.");
        }
        recordAccess.requireStudent(studentId);
        return aiCounselingService.getReportsByStudentId(studentId);
    }

    @PostMapping("/math")
    public AiReportResponse generateMathReport(@RequestParam("studentId") Long studentId) {
        recordAccess.requireStudent(studentId);
        return aiCounselingService.generateMathReport(studentId).block();
    }

    @PostMapping("/writing")
    public AiReportResponse generateWritingReport(@RequestParam("studentId") Long studentId) {
        recordAccess.requireStudent(studentId);
        return aiCounselingService.generateWritingReport(studentId).block();
    }

    @PostMapping("/premium")
    public AiReportResponse generatePremiumReport(@RequestParam("studentId") Long studentId) {
        recordAccess.requireStudent(studentId);
        return aiCounselingService.generatePremiumReport(studentId).block();
    }

    /**
     * 영어/과학/사회 메타인지 분석 리포트.
     * 수학·국어·프리미엄은 위의 고정 경로가 먼저 매칭되므로 여기로 들어오지 않는다.
     */
    @PostMapping("/{subject}")
    public AiReportResponse generateSubjectReport(@PathVariable("subject") String subject,
                                                  @RequestParam("studentId") Long studentId) {
        recordAccess.requireStudent(studentId);
        return aiCounselingService.generateSubjectReport(studentId, subject).block();
    }
}
