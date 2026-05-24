package com.i_route.backend.ai.dto; // 🌟 사용자님의 실제 패키지 경로에 맞췄습니다.

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AiDto {

    // 1. 프론트엔드(React)에서 보낸 질문을 받는 그릇
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String question;
    }

    // 2. 파이썬 FastAPI가 찾아준 수학 족보(Context)를 받는 그릇
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RagResponse {
        private String context;
    }

    // 3. Ollama 엔진 규격에 맞춘 요청 그릇
    @Getter
    @AllArgsConstructor
    public static class OllamaRequest {
        private String model;
        private String prompt;
        private boolean stream;
    }

    // 4. Ollama가 최종 답변을 돌려줄 때 파싱할 그릇
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OllamaResponse {
        private String response;
    }
}