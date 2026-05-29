package com.i_route.backend.ai;

import com.i_route.backend.ai.entity.AiRecommendation;
import com.i_route.backend.ai.repository.AiRecommendationRepository;
import com.i_route.backend.ai.service.GradeAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.Socket;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * AI 서버(localhost:8082) 실제 연동 통합 테스트.
 * 실행 전 AI 서버가 기동 중이어야 합니다.
 */
@ExtendWith(MockitoExtension.class)
class GradeAnalysisServiceIntegrationTest {

    @Mock
    private AiRecommendationRepository aiRecommendationRepository;

    private GradeAnalysisService gradeAnalysisService;

    @BeforeEach
    void setUp() {
        // AI 서버(localhost:8082)가 실행 중이지 않으면 테스트 스킵
        boolean aiServerRunning = false;
        try (Socket s = new Socket("localhost", 8082)) {
            aiServerRunning = true;
        } catch (Exception ignored) {}
        assumeTrue(aiServerRunning, "AI 서버(localhost:8082)가 실행 중이지 않아 통합 테스트를 건너뜁니다.");

        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:8082")
                .build();
        gradeAnalysisService = new GradeAnalysisService(webClient, aiRecommendationRepository);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("[통합] AI 서버 실제 호출 - 취약 개념 추천 후 DB 저장")
    void processStudentGrade_realAiServer_savesConcepts() {
        given(aiRecommendationRepository.save(any(AiRecommendation.class)))
                .willAnswer(inv -> inv.getArgument(0));

        gradeAnalysisService.processStudentGrade(
                "S-001", 60, List.of(50, 60, 70, 80, 90), "이차방정식"
        );

        then(aiRecommendationRepository).should(atLeastOnce()).save(any(AiRecommendation.class));
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("[통합] 평균보다 높은 점수 - AI 서버 호출 후 저장 확인")
    void processStudentGrade_aboveAverage_stillCallsAi() {
        given(aiRecommendationRepository.save(any(AiRecommendation.class)))
                .willAnswer(inv -> inv.getArgument(0));

        gradeAnalysisService.processStudentGrade(
                "S-002", 95, List.of(60, 70, 75, 80, 95), "함수"
        );

        then(aiRecommendationRepository).should(atLeastOnce()).save(any(AiRecommendation.class));
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("[통합] 학생 ID별 AI 추천이 해당 학생 ID로 저장됨")
    void processStudentGrade_savedWithCorrectStudentId() {
        given(aiRecommendationRepository.save(any(AiRecommendation.class)))
                .willAnswer(inv -> inv.getArgument(0));

        gradeAnalysisService.processStudentGrade(
                "S-999", 55, List.of(55, 65, 75), "미적분"
        );

        then(aiRecommendationRepository).should(atLeastOnce()).save(argThat(rec ->
                "S-999".equals(rec.getStudentId())
        ));
    }
}
