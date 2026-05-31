package com.i_route.backend.payment.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

// Toss Payments API 응답 매핑용 (필요한 필드만 선언)
@Getter
@NoArgsConstructor
public class TossConfirmResponse {

    private String paymentKey;
    private String orderId;
    private String orderName;
    private int totalAmount;
    private String status;
    private String method;
    private String requestedAt;
    private String approvedAt;
}
