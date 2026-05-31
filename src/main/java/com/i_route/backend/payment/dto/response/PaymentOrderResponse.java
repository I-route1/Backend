package com.i_route.backend.payment.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentOrderResponse {

    private String orderId;
    private String orderName;
    private int amount;
    private String clientKey;   // 프론트엔드 Toss SDK 초기화에 사용
}
