package com.i_route.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.i_route.backend.ai.dto.AiSearchRequest;
import com.i_route.backend.ai.entity.AiRecommendation;
import com.i_route.backend.ai.repository.AiRecommendationRepository;
import com.i_route.backend.ai.service.GradeAnalysisService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/** 실제 HTTP 직렬화/응답 처리와 저장을 검증하며 외부 AI 서버에는 접속하지 않는다. */
@ExtendWith(MockitoExtension.class)
@Timeout(15)
class GradeAnalysisServiceIntegrationTest {
    @Mock AiRecommendationRepository repository;
    HttpServer server;
    GradeAnalysisService service;
    final ObjectMapper json = new ObjectMapper();
    final List<AiSearchRequest> requests = new CopyOnWriteArrayList<>();
    final List<String> methods = new CopyOnWriteArrayList<>();
    int responseStatus = 200;

    @BeforeEach void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/ai/search", exchange -> {
            methods.add(exchange.getRequestMethod());
            requests.add(json.readValue(exchange.getRequestBody(), AiSearchRequest.class));
            byte[] body = "{\"contexts\":[\"이차방정식 판별식 복습\",\"개념 확인 문제\"]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(responseStatus, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        service = new GradeAnalysisService(WebClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build(), repository);
    }
    @AfterEach void tearDown() { if (server != null) server.stop(0); }

    @Test void processStudentGrade_postsConceptAndPersistsReturnedContexts() {
        var result = service.processStudentGrade(1L, 60, List.of(50,60,70,80,90), "이차방정식");
        assertThat(methods).containsExactly("POST");
        assertThat(requests).singleElement().satisfies(r -> {
            assertThat(r.getQuestion()).isEqualTo("이차방정식");
            assertThat(r.getSubject()).isEqualTo("수학");
        });
        assertThat(result).containsExactly("이차방정식 판별식 복습", "개념 확인 문제");
        then(repository).should(times(2)).save(any(AiRecommendation.class));
    }
    @Test void processStudentGrade_aboveAverageStillCallsAi() {
        service.processStudentGrade(2L,95,List.of(60,70,75,80,95),"함수");
        assertThat(requests).hasSize(1);
        then(repository).should(times(2)).save(any(AiRecommendation.class));
    }
    @Test void processStudentGrade_savesEveryContextForRequestedStudent() {
        service.processStudentGrade(999L,55,List.of(55,65,75),"미적분");
        then(repository).should(times(2)).save(argThat(r -> r.getStudentId().equals(999L)
                && "수학".equals(r.getSubject()) && "수학 맞춤 복습 리포트".equals(r.getTitle())));
        then(repository).should().save(argThat(r -> "이차방정식 판별식 복습".equals(r.getCareerAnalysis())));
        then(repository).should().save(argThat(r -> "개념 확인 문제".equals(r.getCareerAnalysis())));
    }
    @Test void processStudentGrade_aiFailureDoesNotSaveRecommendations() {
        responseStatus = 503;
        assertThatThrownBy(() -> service.processStudentGrade(1L,60,List.of(50,60),"이차방정식"))
                .isInstanceOf(org.springframework.web.reactive.function.client.WebClientResponseException.ServiceUnavailable.class);
        then(repository).shouldHaveNoInteractions();
    }
}
