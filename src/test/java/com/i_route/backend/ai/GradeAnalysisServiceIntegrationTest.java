package com.i_route.backend.ai;

import com.i_route.backend.ai.entity.AiRecommendation;
import com.i_route.backend.ai.repository.AiRecommendationRepository;
import com.i_route.backend.ai.service.GradeAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * 임시 HTTP 서버를 사용하는 WebClient 계약 통합 테스트.
 * 외부 AI 서버나 GPU 없이 HTTP 응답 파싱과 저장을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class GradeAnalysisServiceIntegrationTest {

    @Mock
    private AiRecommendationRepository aiRecommendationRepository;

    private GradeAnalysisService gradeAnalysisService;
    private HttpServer server;
    private final AtomicReference<JsonNode> requestBody = new AtomicReference<>();
    private final AtomicReference<String> requestMethod = new AtomicReference<>();
    @AfterEach void stopServer() { server.stop(0); }

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/ai/search", exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            requestBody.set(new ObjectMapper().readTree(exchange.getRequestBody()));
            byte[] body = "{\"contexts\":[\"concept one\",\"concept two\"]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
            exchange.close();
        });
        server.start();
        WebClient webClient = WebClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .build();
        gradeAnalysisService = new GradeAnalysisService(webClient, aiRecommendationRepository);
    }


    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("[통합] AI HTTP 계약 호출 - 취약 개념 추천 후 DB 저장")
    void processStudentGrade_realAiServer_savesConcepts() {
        given(aiRecommendationRepository.save(any(AiRecommendation.class)))
                .willAnswer(inv -> inv.getArgument(0));

        List<String> result = gradeAnalysisService.processStudentGrade(
                1L, 60, List.of(50, 60, 70, 80, 90), "이차방정식"
        );
        assertThat(result).containsExactly("concept one", "concept two");
        assertThat(requestBody.get().get("question").asText()).isEqualTo("이차방정식");

        assertThat(requestMethod.get()).isEqualTo("POST");
        assertThat(requestBody.get().get("subject").asText()).isEqualTo("수학");
        then(aiRecommendationRepository).should(times(2)).save(any(AiRecommendation.class));
    }


    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("[통합] 평균보다 높은 점수 - AI 서버 호출 후 저장 확인")
    void processStudentGrade_aboveAverage_stillCallsAi() {
        given(aiRecommendationRepository.save(any(AiRecommendation.class)))
                .willAnswer(inv -> inv.getArgument(0));

        gradeAnalysisService.processStudentGrade(
                2L, 95, List.of(60, 70, 75, 80, 95), "함수"
        );

        assertThat(requestMethod.get()).isEqualTo("POST");
        assertThat(requestBody.get().get("subject").asText()).isEqualTo("수학");
        then(aiRecommendationRepository).should(times(2)).save(any(AiRecommendation.class));
    }


    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("[통합] 학생 ID별 AI 추천이 해당 학생 ID로 저장됨")
    void processStudentGrade_savedWithCorrectStudentId() {
        given(aiRecommendationRepository.save(any(AiRecommendation.class)))
                .willAnswer(inv -> inv.getArgument(0));

        gradeAnalysisService.processStudentGrade(
                999L, 55, List.of(55, 65, 75), "미적분"
        );

        then(aiRecommendationRepository).should(times(2)).save(argThat(rec ->
                Long.valueOf(999L).equals(rec.getStudentId())
        ));
    }
}
