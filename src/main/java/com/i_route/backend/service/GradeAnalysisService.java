package com.i_route.backend.service;

import com.i_route.backend.dto.AiSearchRequest;
import com.i_route.backend.dto.AiSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;

@Slf4j
@Service
// 🌟 @RequiredArgsConstructor는 반드시 지워져 있어야 합니다!
public class GradeAnalysisService {

    private final WebClient webClient;

    // 🌟 수동 생성자: 메서드 이름이 클래스명(GradeAnalysisService)과 토시 하나 안 틀리고 완벽히 똑같아야 합니다!
    public GradeAnalysisService(@Qualifier("fastApiWebClient") WebClient webClient) {
        this.webClient = webClient; // 👈 이 대입문 덕분에 '초기화되지 않았을 수 있음' 에러가 3초 만에 소멸합니다.
    }

    public void processStudentGrade(int studentScore, List<Integer> allScores, String weakConceptTag) {
        log.info("📊 [규칙 기반 엔지니어링] 성적 분석 시작...");

        // 1. 순수 자바(규칙 기반)로 평균 및 표준편차 초고속 계산
        double average = allScores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double variance = allScores.stream().mapToDouble(score -> Math.pow(score - average, 2)).average().orElse(0.0);
        double stdDev = Math.sqrt(variance);

        log.info("📈 분석 결과 - 전체 평균: {}, 표준편차: {}", String.format("%.2f", average), String.format("%.2f", stdDev));

        // 2. 실시간 AI RAG 엔진 전송 파트 (아까 뚫어놓은 파이썬 8082 포트로 토스)
        log.info("🚀 취약 개념 TAG [{}] 기반 AI 지식창고 검색 요청 중...", weakConceptTag);

        AiSearchRequest requestBody = new AiSearchRequest(weakConceptTag + " 관련 수학 문제 및 오답 개념 족보 추천해줘");

        // 비동기/논블로킹으로 파이썬 서버 찌르기
        webClient.post()
                .uri("/api/ai/search")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(AiSearchResponse.class)
                .subscribe(response -> {
                    // 파이썬 AI 서버가 2.74GB DB에서 꺼내온 족보 결과가 도착했을 때 실행되는 구역!
                    List<String> recommendedContexts = response.getContexts();
                    log.info("🎯 [AI 서버 응답 도착] 맞춤형 복습 족보 데이터 수신 완료! 건수: {}건", recommendedContexts.size());

                    // TODO: 수신한 족보(Contexts)를 바탕으로 오답노트 테이블에 추천 자료로 실시간 매핑 및 저장
                }, error -> {
                    log.error("❌ AI 서버 통신 에러 발생: {}", error.getMessage());
                });
    }
}