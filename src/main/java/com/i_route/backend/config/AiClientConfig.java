package com.i_route.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AiClientConfig {

    // 1. Ollama (로컬 5070Ti 추론 엔진 포트)
    @Bean
    public WebClient ollamaWebClient(WebClient.Builder builder) {
        return builder.baseUrl("http://localhost:11434").build();
    }

    // 2. 파이썬 FastAPI (FAISS out_db 검색 사이드카 포트)
    @Bean
    public WebClient fastApiWebClient(WebClient.Builder builder) {
        return builder.baseUrl("http://localhost:8000").build();
    }
}