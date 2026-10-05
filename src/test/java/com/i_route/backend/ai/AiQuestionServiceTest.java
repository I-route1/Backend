package com.i_route.backend.ai;

import com.i_route.backend.ai.service.AiQuestionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class AiQuestionServiceTest {

    private final List<ClientRequest> requests = new ArrayList<>();

    /** AI 서버 대신 정해진 상태·본문을 돌려주는 WebClient. */
    private AiQuestionService serviceReturning(HttpStatus status, String json) {
        WebClient client = WebClient.builder()
                .baseUrl("http://ai.test")
                .exchangeFunction(req -> {
                    requests.add(req);
                    return Mono.just(ClientResponse.create(status)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(json).build());
                })
                .build();
        return new AiQuestionService(client);
    }

    private static HttpStatus statusOf(Throwable t) {
        return HttpStatus.valueOf(((ResponseStatusException) t).getStatusCode().value());
    }

    @Test
    @DisplayName("AI 서버 답변을 그대로 돌려주고 /api/ai/ask로 POST한다")
    void ask_success() {
        AiQuestionService service = serviceReturning(HttpStatus.OK,
                "{\"subject\":\"수학\",\"question\":\"판별식이 음수면?\",\"answer\":\"허근이다\",\"grounded\":true,\"needSubject\":false}");

        Map<String, Object> result = service.ask("  판별식이 음수면?  ", "수학");

        assertThat(result.get("answer")).isEqualTo("허근이다");
        assertThat(result.get("subject")).isEqualTo("수학");
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).url().getPath()).isEqualTo("/api/ai/ask");
    }

    @Test
    @DisplayName("과목을 알 수 없으면(needSubject) answer가 없어도 오류가 아니다")
    void ask_needSubject() {
        AiQuestionService service = serviceReturning(HttpStatus.OK,
                "{\"subject\":null,\"question\":\"점심 뭐 먹지\",\"answer\":null,\"grounded\":false,\"needSubject\":true}");

        Map<String, Object> result = service.ask("점심 뭐 먹지", null);

        assertThat(result.get("needSubject")).isEqualTo(true);
    }

    @Test
    @DisplayName("빈 질문·300자 초과·미지원 과목은 AI 서버를 부르지 않고 400")
    void ask_validation() {
        AiQuestionService service = serviceReturning(HttpStatus.OK, "{}");

        assertThatThrownBy(() -> service.ask(" ", null))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.ask("가".repeat(301), null))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.ask("임진왜란은 언제?", "한국사"))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThat(requests).isEmpty();
    }

    @Test
    @DisplayName("AI 서버 5xx나 빈 답변은 503")
    void ask_unavailable() {
        assertThatThrownBy(() -> serviceReturning(HttpStatus.INTERNAL_SERVER_ERROR, "{}").ask("행렬의 곱셈 조건", null))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> serviceReturning(HttpStatus.OK,
                "{\"subject\":\"수학\",\"answer\":null,\"needSubject\":false}").ask("행렬의 곱셈 조건", null))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    @DisplayName("AI 서버 4xx는 400")
    void ask_rejected() {
        assertThatThrownBy(() -> serviceReturning(HttpStatus.BAD_REQUEST, "{\"detail\":\"x\"}").ask("행렬의 곱셈 조건", null))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
