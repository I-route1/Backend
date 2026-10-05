package com.i_route.backend.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 'AI에게 질문하기' — 학생이 쓴 학습 질문을 AI 서버(POST /api/ai/ask)에 넘겨 답을 받는다.
 *
 * 예전 MathAiService(RAG 검색 후 없는 Ollama 모델 "iroute-math-model" 호출)를 대신한다. 그 코드는
 * 어디서도 불리지 않았고, Ollama 연결 설정도 없어 붙여도 동작하지 않았다. AI 서버가 검색·근거·
 * 생성·후처리를 모두 하므로 여기서는 입력 검증과 오류 변환만 한다.
 */
@Slf4j
@Service
public class AiQuestionService {

    /** CloudFront 원본 응답 한도(60초)보다 짧아야 한다 — AiReportController.AI_TIMEOUT과 같은 이유. */
    static final Duration AI_TIMEOUT = Duration.ofSeconds(50);
    static final int MAX_QUESTION_LENGTH = 300;
    static final Set<String> SUPPORTED_SUBJECTS = Set.of("수학", "영어", "국어", "과학", "사회");

    private final WebClient fastApiWebClient;

    public AiQuestionService(@Qualifier("fastApiWebClient") WebClient fastApiWebClient) {
        this.fastApiWebClient = fastApiWebClient;
    }

    /**
     * @return AI 서버 응답 그대로: { subject, question, answer, grounded, needSubject }.
     *         needSubject=true면 과목을 알 수 없어 생성하지 않은 것이다(answer=null).
     */
    public Map<String, Object> ask(String question, String subject) {
        String q = question == null ? "" : question.strip();
        if (q.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문을 입력해 주세요.");
        }
        if (q.length() > MAX_QUESTION_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "질문은 " + MAX_QUESTION_LENGTH + "자 이내로 입력해 주세요.");
        }
        String s = (subject == null || subject.isBlank()) ? null : subject.strip();
        if (s != null && !SUPPORTED_SUBJECTS.contains(s)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 과목입니다: " + s);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("question", q);
        if (s != null) body.put("subject", s);

        Map<String, Object> result;
        try {
            result = fastApiWebClient.post()
                    .uri("/api/ai/ask")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(AI_TIMEOUT);
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                log.warn("[AI 질문] AI 서버가 요청을 거부: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문을 처리할 수 없습니다.");
            }
            log.warn("[AI 질문] AI 서버 오류: {}", e.getStatusCode());
            throw unavailable();
        } catch (Exception e) {
            // 타임아웃(block), 연결 실패 등
            log.warn("[AI 질문] AI 서버 호출 실패: {}", e.getMessage());
            throw unavailable();
        }

        if (result == null) throw unavailable();
        boolean needSubject = Boolean.TRUE.equals(result.get("needSubject"));
        if (!needSubject && result.get("answer") == null) throw unavailable();
        return result;
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "AI 서버가 지금 답변할 수 없습니다. 잠시 후 다시 시도해 주세요.");
    }
}
