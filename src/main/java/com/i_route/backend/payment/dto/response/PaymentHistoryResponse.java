package com.i_route.backend.payment.dto.response;

import com.i_route.backend.payment.entity.Payment;
import com.i_route.backend.payment.entity.PaymentStatus;
import com.i_route.backend.payment.entity.PlanType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentHistoryResponse {

    private Long paymentId;
    private String orderId;
    private PlanType planType;
    private String orderName;
    private int amount;
    private PaymentStatus status;
    private String method;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;

    public static PaymentHistoryResponse from(Payment payment) {
        return PaymentHistoryResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .planType(payment.getPlanType())
                .orderName(payment.getPlanType().getOrderName())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .method(payment.getMethod())
                .createdAt(payment.getCreatedAt())
                .confirmedAt(payment.getConfirmedAt())
                .build();
    }
}
