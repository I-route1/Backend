package com.i_route.backend.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    // CORS는 SecurityConfig.corsConfigurationSource()에서 일원화하여 관리합니다.
    // 중복 CorsFilter가 존재하면 응답에 헤더가 두 번 추가되어 브라우저가 Failed to fetch 오류를 냅니다.
}
