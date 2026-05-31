package com.i_route.backend.payment.entity;

public enum SubscriptionStatus {
    ACTIVE,     // 구독 활성
    CANCELLED,  // 구독 취소
    EXPIRED     // 만료 (결제 실패 등)
}
