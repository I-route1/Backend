package com.i_route.backend.ai.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class AiClientConfig {

    // 🔥 application.yml 또는 properties에서 주소를 읽어옵니다.
    @Value("${ai.server.url}")
    private String aiServerUrl;

    // AI 서버 공유 키. AI 서버를 ngrok 등으로 외부에 열면 주소만 알아도 GPU 생성을 부를 수 있어
    // 이 키를 X-AI-Key 헤더로 보낸다. 비어 있으면 헤더를 붙이지 않는다(AI 서버도 키가 없으면 검사 안 함).
    @Value("${ai.server.key:}")
    private String aiServerKey;

    @Bean(name = "fastApiWebClient")
    public WebClient fastApiWebClient() {

        // 🛑 핵심: 기존의 소중한 3분(180초) 타임아웃 설정은 그대로 완벽하게 유지합니다!
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 180000) // 연결 타임아웃 3분
                .responseTimeout(Duration.ofSeconds(180))            // 응답 타임아웃 3분
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(180, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(180, TimeUnit.SECONDS)));

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(aiServerUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient));
        // trim: 윈도우에서 편집한 .env는 값 끝에 CR 문자가 남아 AI 서버(strip)와 키가 어긋난다.
        if (aiServerKey != null && !aiServerKey.isBlank()) {
            builder.defaultHeader("X-AI-Key", aiServerKey.trim());
        }
        return builder.build();
    }
}