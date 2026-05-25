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

    @Bean(name = "fastApiWebClient")
    public WebClient fastApiWebClient() {

        // 🛑 핵심: 기존의 소중한 3분(180초) 타임아웃 설정은 그대로 완벽하게 유지합니다!
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 180000) // 연결 타임아웃 3분
                .responseTimeout(Duration.ofSeconds(180))            // 응답 타임아웃 3분
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(180, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(180, TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(aiServerUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}