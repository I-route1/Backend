package com.i_route.backend.payment.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

// Toss 빌링키 발급 및 청구 응답 매핑용
@Getter
@NoArgsConstructor
public class TossBillingResponse {

    private String billingKey;
    private String customerKey;
    private String method;
    private String status;
    private String orderId;
    private int totalAmount;
}
