package com.i_route.backend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;
import java.util.Collections;

@Configuration
public class WebConfig {

    @Value("${file.upload-dir:./uploads/profiles/}")
    private String uploadDir;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns(
                                "https://d2nos5u98g310z.cloudfront.net",
                                "https://d2t8h2oy220lpg.cloudfront.net",
                                "https://*.ngrok-free.dev",
                                "http://localhost:3000"
                        )
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("*")
                        .allowCredentials(true);
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                String absolutePath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
                registry.addResourceHandler("/uploads/profiles/**")
                        .addResourceLocations(absolutePath);
            }

            @Override
            public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
                configurer.defaultContentTypeStrategy((request) ->
                        Collections.singletonList(MediaType.APPLICATION_JSON)
                );
            }
        };
    }
}