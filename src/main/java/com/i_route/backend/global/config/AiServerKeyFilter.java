package com.i_route.backend.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * /api/wrong-answer/ai-pipeline은 AI 서버 전용 내부 API라 SecurityConfig에서 permitAll이다
 * (Python이 JWT를 안 갖고 있어서). 그런데 그 말은 주소를 아는 사람이면 누구나 어느 학생의
 * 취약 개념이든 조회할 수 있었다는 뜻이다.
 *
 * AI 서버가 이미 들어오는 요청을 막는 데 쓰는 것과 같은 값(ai.server.key = AI_SERVER_KEY
 * 환경변수)을 여기서도 쓴다. AI 서버(java_client.py)가 나가는 요청에 X-AI-Key 헤더로 같은
 * 값을 보내면 이 필터가 확인한다 — 새 비밀값을 하나 더 관리하지 않으려는 것이다. 빈 값이면
 * (로컬 개발 등 키를 안 정한 경우) 예전처럼 검사하지 않는다.
 */
@Component
public class AiServerKeyFilter extends OncePerRequestFilter {

    private static final String PROTECTED_PATH = "/api/wrong-answer/ai-pipeline";

    @Value("${ai.server.key:}")
    private String aiServerKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        boolean guarded = aiServerKey != null && !aiServerKey.isBlank()
                && PROTECTED_PATH.equals(request.getRequestURI());

        if (guarded && !constantTimeEquals(aiServerKey.trim(), request.getHeader("X-AI-Key"))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"X-AI-Key가 없거나 틀렸습니다.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}
