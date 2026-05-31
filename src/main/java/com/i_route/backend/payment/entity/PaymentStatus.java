package com.i_route.backend.payment.entity;

public enum PaymentStatus {
    PENDING,    // 주문 생성, 결제 대기
    DONE,       // 결제 완료
    CANCELLED,  // 취소
    FAILED      // 실패
}
