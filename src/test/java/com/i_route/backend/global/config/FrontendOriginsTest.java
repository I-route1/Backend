package com.i_route.backend.global.config;

import com.i_route.backend.global.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FrontendOriginsTest {
    private static final String ORIGINAL_FRONTEND = "https://d2nos5u98g310z.cloudfront.net";
    private static final String NGROK_PATTERN = "https://*.ngrok-free.dev";

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankOriginPreservesEachExistingOriginList(String configuredOrigin) {
        var configurations = configurations(new FrontendOrigins(configuredOrigin));
        assertThat(configurations.get(0).getAllowedOriginPatterns()).containsExactly(
                "http://localhost:3000", "http://localhost:5173", "http://127.0.0.1:3000",
                "http://127.0.0.1:5173", ORIGINAL_FRONTEND, NGROK_PATTERN);
        assertThat(configurations.get(1).getAllowedOriginPatterns()).containsExactly(
                ORIGINAL_FRONTEND, "https://d2t8h2oy220lpg.cloudfront.net", NGROK_PATTERN, "http://localhost:3000");
        assertThat(configurations.get(2).getAllowedOriginPatterns()).containsExactly(
                "http://localhost:3000", "http://localhost:5173", ORIGINAL_FRONTEND, NGROK_PATTERN);
    }

    @Test
    void configuredOriginIsAllowedExactlyInHttpMvcAndWebSocket() {
        String origin = "https://new-frontend.example";
        var configurations = configurations(new FrontendOrigins("https://NEW-frontend.example:443"));
        for (CorsConfiguration configuration : configurations) {
            assertThat(configuration.checkOrigin(origin)).isEqualTo(origin);
            assertThat(configuration.checkOrigin(ORIGINAL_FRONTEND)).isEqualTo(ORIGINAL_FRONTEND);
            assertThat(configuration.checkOrigin(origin + ".other.example")).isNull();
            assertThat(configuration.checkOrigin("http://new-frontend.example")).isNull();
            assertThat(configuration.checkOrigin(origin + ":8443")).isNull();
        }
        assertThat(configurations.get(0).getAllowCredentials()).isTrue();
        assertThat(configurations.get(1).getAllowCredentials()).isTrue();
        assertThat(configurations.get(0).getAllowedMethods()).contains("HEAD", "OPTIONS", "PATCH");
        assertThat(configurations.get(1).getAllowedMethods()).contains("OPTIONS", "PATCH");
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "https://*.example", "https://frontend.example:[443,8443]",
            "https://frontend.example:0", "https://frontend.example:65536", "http://frontend.example",
            "https://login@frontend.example", "https://frontend.example/", "https://frontend.example/path",
            "https://frontend.example?x", "https://frontend.example#x", "https://first.example,https://second.example"})
    void rejectsValuesThatAreNotASingleHttpsOrigin(String configuredOrigin) {
        assertThatIllegalArgumentException().isThrownBy(() -> new FrontendOrigins(configuredOrigin));
    }

    @Test
    void applicationYamlMapsDeploymentEnvironmentKeys() throws IOException {
        var yaml = new YamlPropertySourceLoader().load("application-yaml",
                new FileSystemResource("src/main/resources/application.yml"));
        new ApplicationContextRunner().withInitializer(context -> {
            var sources = context.getEnvironment().getPropertySources();
            sources.addFirst(new MapPropertySource("deployment-environment", Map.of(
                    "FRONTEND_ADDITIONAL_ORIGIN", "https://new-frontend.example",
                    "DEMO_SEED_ENABLED", "false")));
            yaml.forEach(sources::addLast);
        }).withUserConfiguration(FrontendOrigins.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(FrontendOrigins.class).includeIn(ORIGINAL_FRONTEND))
                    .containsExactly(ORIGINAL_FRONTEND, "https://new-frontend.example");
            assertThat(context.getEnvironment().getProperty("app.demo-seed.enabled", Boolean.class)).isFalse();
        });
    }

    private List<CorsConfiguration> configurations(FrontendOrigins origins) {
        var security = new SecurityConfig(mock(JwtAuthenticationFilter.class), mock(AiServerKeyFilter.class), origins);
        var source = (UrlBasedCorsConfigurationSource) security.corsConfigurationSource();
        var mvc = new TestCorsRegistry();
        new WebConfig(origins).corsConfigurer().addCorsMappings(mvc);

        var registry = mock(StompEndpointRegistry.class);
        var endpoint = mock(StompWebSocketEndpointRegistration.class);
        given(registry.addEndpoint("/ws")).willReturn(endpoint);
        new WebSocketConfig(origins).registerStompEndpoints(registry);
        var patterns = ArgumentCaptor.forClass(String[].class);
        verify(endpoint).setAllowedOriginPatterns(patterns.capture());
        var webSocket = new CorsConfiguration();
        webSocket.setAllowedOriginPatterns(List.of(patterns.getValue()));
        return List.of(source.getCorsConfigurations().get("/**"), mvc.configuration(), webSocket);
    }

    private static class TestCorsRegistry extends CorsRegistry {
        CorsConfiguration configuration() {
            return getCorsConfigurations().get("/**");
        }
    }
}
