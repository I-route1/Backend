package com.i_route.backend.controller;

import com.i_route.backend.dto.AiReportRequest;
import com.i_route.backend.dto.AiReportResponse;
import com.i_route.backend.service.AiCounselingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/counseling")
@RequiredArgsConstructor
public class AiCounselingController {

    private final AiCounselingService aiCounselingService;

    @PostMapping("/generate-report")
    public Mono<ResponseEntity<AiReportResponse>> generateReport(@RequestBody AiReportRequest request) {
        // 프론트엔드에서 넘어온 요청을 Service로 넘겨서 파이썬 서버와 통신하게 함
        return aiCounselingService.generatePremiumReport(request)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }
}