package com.i_route.backend.service;

import com.i_route.backend.dto.AiReportRequest;
import com.i_route.backend.dto.AiReportResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class AiCounselingService {

    private final WebClient webClient;

    // 아까 만들어둔 파이썬 전용 WebClient 주입
    public AiCounselingService(@Qualifier("fastApiWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<AiReportResponse> generatePremiumReport(AiReportRequest request) {
        log.info("🚀 [AI 컨설팅 요청] 학생 ID: {} 의 프리미엄 리포트 생성을 파이썬 서버에 요청합니다...", request.getStudentId());

        return webClient.post()
                .uri("/api/ai/report")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiReportResponse.class)
                .doOnSuccess(response -> log.info("✅ [AI 응답 완료] 리포트 생성 성공!"))
                .doOnError(error -> log.error("❌ [AI 통신 에러] 파이썬 서버 응답 실패: {}", error.getMessage()));
    }
}