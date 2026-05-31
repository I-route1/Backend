package com.i_route.backend.payment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlanType {

    PREMIUM_REPORT(5000, "AI 프리미엄 통합 리포트", 1, false),
    MONTHLY_BASIC(9900, "i-Route 베이직 구독", 0, true),
    MONTHLY_PREMIUM(19900, "i-Route 프리미엄 구독", 0, true);

    private final int amount;
    private final String orderName;
    private final int creditsToAdd;   // 일반 결제 시 지급 크레딧
    private final boolean subscription; // 정기 결제 여부
}
