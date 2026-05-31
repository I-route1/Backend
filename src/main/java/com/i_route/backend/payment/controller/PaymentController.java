package com.i_route.backend.payment.controller;

import com.i_route.backend.global.security.CustomUserDetails;
import com.i_route.backend.payment.dto.request.BillingIssueRequest;
import com.i_route.backend.payment.dto.request.PaymentConfirmRequest;
import com.i_route.backend.payment.dto.request.PaymentOrderRequest;
import com.i_route.backend.payment.dto.response.PaymentHistoryResponse;
import com.i_route.backend.payment.dto.response.PaymentOrderResponse;
import com.i_route.backend.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 일반 결제 주문 생성
     * POST /api/payments/order
     * → orderId, clientKey 반환 후 프론트에서 Toss SDK 호출
     */
    @PostMapping("/order")
    public ResponseEntity<PaymentOrderResponse> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentOrderRequest request) {
        return ResponseEntity.ok(paymentService.createOrder(userDetails.getId(), request));
    }

    /**
     * 일반 결제 승인
     * POST /api/payments/confirm
     * → Toss SDK 성공 후 프론트가 paymentKey, orderId, amount 전달
     */
    @PostMapping("/confirm")
    public ResponseEntity<PaymentHistoryResponse> confirmPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentConfirmRequest request) {
        return ResponseEntity.ok(paymentService.confirmPayment(userDetails.getId(), request));
    }

    /**
     * 정기 결제 빌링키 발급 + 첫 달 즉시 결제
     * POST /api/payments/billing/issue
     */
    @PostMapping("/billing/issue")
    public ResponseEntity<Void> issueBilling(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody BillingIssueRequest request) {
        paymentService.issueBillingKey(userDetails.getId(), request);
        return ResponseEntity.ok().build();
    }

    /**
     * 구독 취소
     * DELETE /api/payments/billing
     */
    @DeleteMapping("/billing")
    public ResponseEntity<Void> cancelSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        paymentService.cancelSubscription(userDetails.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * 결제 내역 조회
     * GET /api/payments/history
     */
    @GetMapping("/history")
    public ResponseEntity<List<PaymentHistoryResponse>> getHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(paymentService.getHistory(userDetails.getId()));
    }
}
