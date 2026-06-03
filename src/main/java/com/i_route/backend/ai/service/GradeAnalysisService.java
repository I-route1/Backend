package com.i_route.backend.ai.service;

import com.i_route.backend.ai.dto.AiSearchRequest;
import com.i_route.backend.ai.dto.AiSearchResponse;
import com.i_route.backend.ai.entity.AiRecommendation;
import com.i_route.backend.ai.repository.AiRecommendationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;

@Slf4j
@Service
public class GradeAnalysisService {

    private final WebClient webClient;
    private final AiRecommendationRepository aiRecommendationRepository; // 🌟 추가: DB 저장소 의존성

    // 🌟 생성자에 Repository 주입 추가
    public GradeAnalysisService(@Qualifier("fastApiWebClient") WebClient webClient,
                                AiRecommendationRepository aiRecommendationRepository) {
        this.webClient = webClient;
        this.aiRecommendationRepository = aiRecommendationRepository;
    }

    /**
     * [규칙 기반 처리 + AI 연동 + DB 저장]
     */
    public List<String> processStudentGrade(String studentId, int studentScore, List<Integer> allScores, String weakConceptTag) {
        log.info("📊 [규칙 기반 엔지니어링] 성적 분석 시작...");

        // 1. 순수 자바(규칙 기반)로 평균 및 표준편차 초고속 계산
        double average = allScores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double variance = allScores.stream().mapToDouble(score -> Math.pow(score - average, 2)).average().orElse(0.0);
        double stdDev = Math.sqrt(variance);

        log.info("📈 분석 결과 - 전체 평균: {}, 표준편차: {}", String.format("%.2f", average), String.format("%.2f", stdDev));

        // 2. 실시간 AI RAG 엔진 전송 파트
        log.info("🚀 취약 개념 TAG [{}] 기반 AI 지식창고 검색 요청 중...", weakConceptTag);

        AiSearchRequest requestBody = new AiSearchRequest(weakConceptTag + " 관련 수학 문제 및 오답 개념 족보 추천해줘");

        // 파이썬 서버 응답을 받은 후 DB 저장까지 완료하고 반환
        AiSearchResponse response = webClient.post()
                .uri("/api/ai/search")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(AiSearchResponse.class)
                .doOnError(error -> log.error("❌ AI 서버 통신 에러 발생: {}", error.getMessage()))
                .block();

        if (response != null) {
            List<String> recommendedContexts = response.getContexts();
            log.info("🎯 [AI 서버 응답 도착] 맞춤형 복습 족보 데이터 수신 완료! 건수: {}건", recommendedContexts.size());

            for (String context : recommendedContexts) {
                AiRecommendation recommendation = AiRecommendation.builder()
                        .studentId(studentId)
                        .careerAnalysis(context)
                        .build();

                aiRecommendationRepository.save(recommendation);
            }
            log.info("💾 AI 맞춤 족보 {}건 DB 저장 완료!", recommendedContexts.size());
            return recommendedContexts;
        }
        return List.of();
    }
}