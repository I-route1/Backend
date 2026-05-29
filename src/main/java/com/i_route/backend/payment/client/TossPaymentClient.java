package com.i_route.backend.payment.client;

import com.i_route.backend.payment.dto.response.TossBillingResponse;
import com.i_route.backend.payment.dto.response.TossConfirmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
public class TossPaymentClient {

    private final WebClient webClient;

    public TossPaymentClient(@Value("${toss.secret-key}") String secretKey) {
        String encoded = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        this.webClient = WebClient.builder()
                .baseUrl("https://api.tosspayments.com")
                .defaultHeader("Authorization", "Basic " + encoded)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    // 일반 결제 승인
    public TossConfirmResponse confirmPayment(String paymentKey, String orderId, int amount) {
        return webClient.post()
                .uri("/v1/payments/confirm")
                .bodyValue(Map.of(
                        "paymentKey", paymentKey,
                        "orderId", orderId,
                        "amount", amount
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, res -> res.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("[Toss] 결제 승인 실패: {}", body);
                            return Mono.error(new RuntimeException("Toss 결제 승인 실패: " + body));
                        }))
                .bodyToMono(TossConfirmResponse.class)
                .block();
    }

    // 빌링키 발급 (카드 등록)
    public TossBillingResponse issueBillingKey(String authKey, String customerKey) {
        return webClient.post()
                .uri("/v1/billing/authorizations/issue")
                .bodyValue(Map.of(
                        "authKey", authKey,
                        "customerKey", customerKey
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, res -> res.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("[Toss] 빌링키 발급 실패: {}", body);
                            return Mono.error(new RuntimeException("Toss 빌링키 발급 실패: " + body));
                        }))
                .bodyToMono(TossBillingResponse.class)
                .block();
    }

    // 빌링키로 정기 결제 청구
    public TossBillingResponse chargeBilling(String billingKey, String customerKey,
                                              String orderId, String orderName, int amount) {
        return webClient.post()
                .uri("/v1/billing/{billingKey}", billingKey)
                .bodyValue(Map.of(
                        "customerKey", customerKey,
                        "amount", amount,
                        "orderId", orderId,
                        "orderName", orderName
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, res -> res.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("[Toss] 정기 결제 청구 실패: {}", body);
                            return Mono.error(new RuntimeException("Toss 정기 결제 청구 실패: " + body));
                        }))
                .bodyToMono(TossBillingResponse.class)
                .block();
    }
}
