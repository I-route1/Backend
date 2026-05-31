package com.i_route.backend.payment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlanType {

    PREMIUM_REPORT(3000, "AI 프리미엄 통합 리포트 1회", 1, false),
    PREMIUM_REPORT_10(27000, "AI 프리미엄 통합 리포트 10회 패키지", 11, false),
    MONTHLY_BASIC(7000, "i-Route 베이직 구독 1개월", 0, true),
    MONTHLY_BASIC_ANNUAL(70000, "i-Route 베이직 구독 12개월", 0, false),
    MONTHLY_PREMIUM(14900, "i-Route 프리미엄 구독 1개월", 0, true);

    private final int amount;
    private final String orderName;
    private final int creditsToAdd;   // 일반 결제 시 지급 크레딧
    private final boolean subscription; // 정기 결제 여부
}
