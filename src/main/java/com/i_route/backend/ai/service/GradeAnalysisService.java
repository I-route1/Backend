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
    private String detectSubject(String tag) {
        if (tag == null) return "수학";
        if (tag.contains("영어") || tag.contains("grammar") || tag.contains("reading")) return "영어";
        if (tag.contains("국어") || tag.contains("문학") || tag.contains("비문학")) return "국어";
        if (tag.contains("과학") || tag.contains("물리") || tag.contains("화학") || tag.contains("생물")) return "과학";
        if (tag.contains("사회") || tag.contains("역사") || tag.contains("지리") || tag.contains("경제")) return "사회";
        if (tag.contains("한국사")) return "한국사";
        return "수학";
    }

    public List<String> processStudentGrade(String studentId, int studentScore, List<Integer> allScores, String weakConceptTag) {
        log.info("📊 [규칙 기반 엔지니어링] 성적 분석 시작...");

        // 1. 순수 자바(규칙 기반)로 평균 및 표준편차 초고속 계산
        double average = allScores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double variance = allScores.stream().mapToDouble(score -> Math.pow(score - average, 2)).average().orElse(0.0);
        double stdDev = Math.sqrt(variance);

        log.info("📈 분석 결과 - 전체 평균: {}, 표준편차: {}", String.format("%.2f", average), String.format("%.2f", stdDev));

        // 2. 실시간 AI RAG 엔진 전송 파트
        log.info("🚀 취약 개념 TAG [{}] 기반 AI 지식창고 검색 요청 중...", weakConceptTag);

        // weakConceptTag가 과목명 단독이면 기본 개념으로 보강 (사회탐구/과학탐구 포함)
        boolean isSubjectOnly = weakConceptTag != null &&
                weakConceptTag.matches("수학|영어|국어|과학|과학탐구|사회|사회탐구|한국사");
        // 사회탐구 → 사회, 과학탐구 → 과학으로 AI 서버 인식 가능한 과목명으로 정규화
        String subject = isSubjectOnly
                ? weakConceptTag.replace("사회탐구", "사회").replace("과학탐구", "과학")
                : detectSubject(weakConceptTag);
        String conceptQuery = isSubjectOnly
                ? subject + " 핵심 개념 및 자주 출제되는 단원"
                : weakConceptTag;
        AiSearchRequest requestBody = new AiSearchRequest(conceptQuery, subject);

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