package com.i_route.backend.payment.service;

import com.i_route.backend.payment.client.TossPaymentClient;
import com.i_route.backend.payment.dto.request.BillingIssueRequest;
import com.i_route.backend.payment.dto.request.PaymentConfirmRequest;
import com.i_route.backend.payment.dto.request.PaymentOrderRequest;
import com.i_route.backend.payment.dto.response.PaymentHistoryResponse;
import com.i_route.backend.payment.dto.response.PaymentOrderResponse;
import com.i_route.backend.payment.dto.response.TossBillingResponse;
import com.i_route.backend.payment.dto.response.TossConfirmResponse;
import com.i_route.backend.payment.entity.*;
import com.i_route.backend.payment.repository.PaymentRepository;
import com.i_route.backend.payment.repository.SubscriptionRepository;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final TossPaymentClient tossClient;

    @Value("${toss.client-key}")
    private String clientKey;

    // ── 일반 결제 ─────────────────────────────────────────────────

    @Transactional
    public PaymentOrderResponse createOrder(Long userId, PaymentOrderRequest request) {
        PlanType plan = request.getPlanType();

        if (plan.isSubscription()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "구독 플랜은 /billing/issue 를 사용하세요.");
        }

        String orderId = UUID.randomUUID().toString();

        paymentRepository.save(Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .planType(plan)
                .amount(plan.getAmount())
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build());

        return PaymentOrderResponse.builder()
                .orderId(orderId)
                .orderName(plan.getOrderName())
                .amount(plan.getAmount())
                .clientKey(clientKey)
                .build();
    }

    @Transactional
    public PaymentHistoryResponse confirmPayment(Long userId, PaymentConfirmRequest request) {
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));

        if (!payment.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 결제만 승인할 수 있습니다.");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리된 주문입니다.");
        }
        if (payment.getAmount() != request.getAmount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다.");
        }

        try {
            TossConfirmResponse tossRes = tossClient.confirmPayment(
                    request.getPaymentKey(), request.getOrderId(), request.getAmount());

            payment.confirm(tossRes.getPaymentKey(), tossRes.getMethod());

            // 프리미엄 리포트 결제 시 크레딧 지급
            int credits = payment.getPlanType().getCreditsToAdd();
            if (credits > 0) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
                user.setPremiumCredits(user.getPremiumCredits() + credits);
                userRepository.save(user);
                log.info("[결제] userId={} 크레딧 {}개 지급 (총 {}개)", userId, credits, user.getPremiumCredits());
            }

            log.info("[결제 완료] orderId={} userId={} amount={}", payment.getOrderId(), userId, payment.getAmount());
            return PaymentHistoryResponse.from(payment);

        } catch (Exception e) {
            payment.fail(e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "결제 승인 중 오류가 발생했습니다.");
        }
    }

    // ── 정기 결제 ─────────────────────────────────────────────────

    @Transactional
    public void issueBillingKey(Long userId, BillingIssueRequest request) {
        PlanType plan = request.getPlanType();

        if (!plan.isSubscription()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "구독 플랜이 아닙니다. MONTHLY_BASIC 또는 MONTHLY_PREMIUM을 선택하세요.");
        }

        // 이미 활성 구독이 있으면 중복 방지
        subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .ifPresent(s -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 활성 구독이 존재합니다."); });

        String customerKey = "customer_" + userId;

        TossBillingResponse tossRes = tossClient.issueBillingKey(request.getAuthKey(), customerKey);

        // 빌링키 발급 즉시 첫 달 결제
        String orderId = UUID.randomUUID().toString();
        TossBillingResponse chargeRes = tossClient.chargeBilling(
                tossRes.getBillingKey(), customerKey, orderId, plan.getOrderName(), plan.getAmount());

        paymentRepository.save(Payment.builder()
                .orderId(orderId)
                .paymentKey(chargeRes.getOrderId())
                .userId(userId)
                .planType(plan)
                .amount(plan.getAmount())
                .status(PaymentStatus.DONE)
                .method(chargeRes.getMethod())
                .createdAt(LocalDateTime.now())
                .confirmedAt(LocalDateTime.now())
                .build());

        subscriptionRepository.save(Subscription.builder()
                .userId(userId)
                .billingKey(tossRes.getBillingKey())
                .customerKey(customerKey)
                .planType(plan)
                .status(SubscriptionStatus.ACTIVE)
                .nextBillingDate(LocalDate.now().plusMonths(1))
                .createdAt(LocalDateTime.now())
                .build());

        log.info("[구독 시작] userId={} plan={} 다음 청구일={}", userId, plan, LocalDate.now().plusMonths(1));
    }

    @Transactional
    public void cancelSubscription(Long userId) {
        Subscription sub = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "활성 구독이 없습니다."));

        sub.cancel();
        log.info("[구독 취소] userId={} subscriptionId={}", userId, sub.getId());
    }

    // 매일 오전 9시 정기 결제 자동 청구
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void processScheduledBilling() {
        List<Subscription> targets = subscriptionRepository
                .findByStatusAndNextBillingDateLessThanEqual(SubscriptionStatus.ACTIVE, LocalDate.now());

        log.info("[정기 결제] 청구 대상 {}건", targets.size());

        for (Subscription sub : targets) {
            String orderId = UUID.randomUUID().toString();
            try {
                TossBillingResponse res = tossClient.chargeBilling(
                        sub.getBillingKey(), sub.getCustomerKey(),
                        orderId, sub.getPlanType().getOrderName(), sub.getPlanType().getAmount());

                paymentRepository.save(Payment.builder()
                        .orderId(orderId)
                        .paymentKey(res.getOrderId())
                        .userId(sub.getUserId())
                        .planType(sub.getPlanType())
                        .amount(sub.getPlanType().getAmount())
                        .status(PaymentStatus.DONE)
                        .method(res.getMethod())
                        .createdAt(LocalDateTime.now())
                        .confirmedAt(LocalDateTime.now())
                        .build());

                sub.renewBillingDate();
                log.info("[정기 결제 완료] userId={} 다음 청구일={}", sub.getUserId(), sub.getNextBillingDate());

            } catch (Exception e) {
                sub.expire();
                log.error("[정기 결제 실패] userId={} 구독 만료 처리: {}", sub.getUserId(), e.getMessage());
            }
        }
    }

    // ── 조회 ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> getHistory(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(PaymentHistoryResponse::from)
                .collect(Collectors.toList());
    }
}
