package com.i_route.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** POST /api/ai/ask 요청. subject가 비어 있으면 AI 서버가 질문에서 과목을 감지한다. */
public class AiQuestionDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String question;
        private String subject;
    }
}
