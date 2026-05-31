package com.i_route.backend.payment;

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
import com.i_route.backend.payment.service.PaymentService;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock private PaymentRepository paymentRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private UserRepository userRepository;
    @Mock private TossPaymentClient tossClient;

    // ── 헬퍼 ─────────────────────────────────────────────────────

    private PaymentOrderRequest orderRequest(PlanType planType) {
        PaymentOrderRequest req = new PaymentOrderRequest();
        ReflectionTestUtils.setField(req, "planType", planType);
        return req;
    }

    private PaymentConfirmRequest confirmRequest(String paymentKey, String orderId, int amount) {
        PaymentConfirmRequest req = new PaymentConfirmRequest();
        ReflectionTestUtils.setField(req, "paymentKey", paymentKey);
        ReflectionTestUtils.setField(req, "orderId", orderId);
        ReflectionTestUtils.setField(req, "amount", amount);
        return req;
    }

    private BillingIssueRequest billingRequest(String authKey, PlanType planType) {
        BillingIssueRequest req = new BillingIssueRequest();
        ReflectionTestUtils.setField(req, "authKey", authKey);
        ReflectionTestUtils.setField(req, "planType", planType);
        return req;
    }

    private Payment pendingPayment(String orderId, Long userId, PlanType planType) {
        return Payment.builder()
                .id(1L)
                .orderId(orderId)
                .userId(userId)
                .planType(planType)
                .amount(planType.getAmount())
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Payment donePayment(String orderId, Long userId, PlanType planType) {
        return Payment.builder()
                .id(2L)
                .orderId(orderId)
                .userId(userId)
                .planType(planType)
                .amount(planType.getAmount())
                .status(PaymentStatus.DONE)
                .method("카드")
                .createdAt(LocalDateTime.now())
                .confirmedAt(LocalDateTime.now())
                .build();
    }

    private User user(Long id, int credits) {
        return User.builder()
                .id(id)
                .nickname("테스트유저")
                .email("test@test.com")
                .role(User.UserRole.PARENT)
                .loginType(User.LoginType.EMAIL)
                .premiumCredits(credits)
                .build();
    }

    private TossConfirmResponse tossConfirmResponse() {
        TossConfirmResponse res = new TossConfirmResponse();
        ReflectionTestUtils.setField(res, "paymentKey", "tviva20241234567890");
        ReflectionTestUtils.setField(res, "method", "카드");
        ReflectionTestUtils.setField(res, "status", "DONE");
        return res;
    }

    private TossBillingResponse tossBillingResponse() {
        TossBillingResponse res = new TossBillingResponse();
        ReflectionTestUtils.setField(res, "billingKey", "billing_key_123");
        ReflectionTestUtils.setField(res, "customerKey", "customer_1");
        ReflectionTestUtils.setField(res, "method", "카드");
        ReflectionTestUtils.setField(res, "status", "DONE");
        ReflectionTestUtils.setField(res, "orderId", "order_123");
        return res;
    }

    private Subscription activeSubscription(Long userId, PlanType planType, LocalDate nextBillingDate) {
        return Subscription.builder()
                .id(1L)
                .userId(userId)
                .billingKey("billing_key_123")
                .customerKey("customer_" + userId)
                .planType(planType)
                .status(SubscriptionStatus.ACTIVE)
                .nextBillingDate(nextBillingDate)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── createOrder ──────────────────────────────────────────────

    @Test
    @DisplayName("일반 결제 주문 생성 성공")
    void createOrder_success() {
        ReflectionTestUtils.setField(paymentService, "clientKey", "test_ck_xxx");
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

        PaymentOrderResponse result = paymentService.createOrder(1L, orderRequest(PlanType.PREMIUM_REPORT));

        assertThat(result.getOrderName()).isEqualTo("AI 프리미엄 통합 리포트");
        assertThat(result.getAmount()).isEqualTo(5000);
        assertThat(result.getClientKey()).isEqualTo("test_ck_xxx");
        assertThat(result.getOrderId()).isNotBlank();
    }

    @Test
    @DisplayName("일반 결제 주문 생성 실패 - 구독 플랜 요청")
    void createOrder_subscriptionPlan_throws400() {
        assertThatThrownBy(() -> paymentService.createOrder(1L, orderRequest(PlanType.MONTHLY_BASIC)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("구독 플랜");
    }

    // ── confirmPayment ───────────────────────────────────────────

    @Test
    @DisplayName("결제 승인 성공 - 크레딧 지급")
    void confirmPayment_success_grantsCredits() {
        Payment payment = pendingPayment("order-123", 1L, PlanType.PREMIUM_REPORT);
        User u = user(1L, 0);

        given(paymentRepository.findByOrderId("order-123")).willReturn(Optional.of(payment));
        given(tossClient.confirmPayment(any(), any(), anyInt())).willReturn(tossConfirmResponse());
        given(userRepository.findById(1L)).willReturn(Optional.of(u));
        given(userRepository.save(any(User.class))).willReturn(u);

        PaymentHistoryResponse result = paymentService.confirmPayment(1L,
                confirmRequest("tviva20241234567890", "order-123", 5000));

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.DONE);
        assertThat(u.getPremiumCredits()).isEqualTo(1);
    }

    @Test
    @DisplayName("결제 승인 실패 - 존재하지 않는 주문")
    void confirmPayment_notFound() {
        given(paymentRepository.findByOrderId("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPayment(1L,
                confirmRequest("key", "unknown", 5000)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("주문을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("결제 승인 실패 - 타인의 주문")
    void confirmPayment_forbidden() {
        Payment payment = pendingPayment("order-123", 99L, PlanType.PREMIUM_REPORT);
        given(paymentRepository.findByOrderId("order-123")).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirmPayment(1L,
                confirmRequest("key", "order-123", 5000)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("본인의 결제만");
    }

    @Test
    @DisplayName("결제 승인 실패 - 이미 완료된 주문")
    void confirmPayment_alreadyDone() {
        Payment payment = donePayment("order-123", 1L, PlanType.PREMIUM_REPORT);
        given(paymentRepository.findByOrderId("order-123")).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirmPayment(1L,
                confirmRequest("key", "order-123", 5000)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 처리된 주문");
    }

    @Test
    @DisplayName("결제 승인 실패 - 금액 불일치")
    void confirmPayment_amountMismatch() {
        Payment payment = pendingPayment("order-123", 1L, PlanType.PREMIUM_REPORT);
        given(paymentRepository.findByOrderId("order-123")).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirmPayment(1L,
                confirmRequest("key", "order-123", 9999)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("금액이 일치하지 않습니다");
    }

    @Test
    @DisplayName("결제 승인 실패 - Toss API 오류 시 FAILED 상태로 저장")
    void confirmPayment_tossApiError_savesFailedStatus() {
        Payment payment = pendingPayment("order-123", 1L, PlanType.PREMIUM_REPORT);
        given(paymentRepository.findByOrderId("order-123")).willReturn(Optional.of(payment));
        given(tossClient.confirmPayment(any(), any(), anyInt()))
                .willThrow(new RuntimeException("Toss API 오류"));

        assertThatThrownBy(() -> paymentService.confirmPayment(1L,
                confirmRequest("key", "order-123", 5000)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("결제 승인 중 오류");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    // ── issueBillingKey ──────────────────────────────────────────

    @Test
    @DisplayName("구독 등록 성공 - 빌링키 발급 + 첫 달 즉시 결제")
    void issueBillingKey_success() {
        given(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .willReturn(Optional.empty());
        given(tossClient.issueBillingKey(any(), any())).willReturn(tossBillingResponse());
        given(tossClient.chargeBilling(any(), any(), any(), any(), anyInt()))
                .willReturn(tossBillingResponse());
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));
        given(subscriptionRepository.save(any(Subscription.class))).willAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> paymentService.issueBillingKey(1L,
                billingRequest("auth_key_123", PlanType.MONTHLY_BASIC)))
                .doesNotThrowAnyException();

        then(subscriptionRepository).should().save(any(Subscription.class));
        then(paymentRepository).should().save(any(Payment.class));
    }

    @Test
    @DisplayName("구독 등록 실패 - 일반 결제 플랜 요청")
    void issueBillingKey_nonSubscriptionPlan_throws400() {
        assertThatThrownBy(() -> paymentService.issueBillingKey(1L,
                billingRequest("auth_key", PlanType.PREMIUM_REPORT)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("구독 플랜이 아닙니다");
    }

    @Test
    @DisplayName("구독 등록 실패 - 이미 활성 구독 존재")
    void issueBillingKey_alreadyActive_throws409() {
        Subscription existing = activeSubscription(1L, PlanType.MONTHLY_BASIC, LocalDate.now().plusMonths(1));
        given(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .willReturn(Optional.of(existing));

        assertThatThrownBy(() -> paymentService.issueBillingKey(1L,
                billingRequest("auth_key", PlanType.MONTHLY_BASIC)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 활성 구독");
    }

    // ── cancelSubscription ───────────────────────────────────────

    @Test
    @DisplayName("구독 취소 성공")
    void cancelSubscription_success() {
        Subscription sub = activeSubscription(1L, PlanType.MONTHLY_BASIC, LocalDate.now().plusMonths(1));
        given(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .willReturn(Optional.of(sub));

        paymentService.cancelSubscription(1L);

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(sub.getCancelledAt()).isNotNull();
    }

    @Test
    @DisplayName("구독 취소 실패 - 활성 구독 없음")
    void cancelSubscription_notFound() {
        given(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.cancelSubscription(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("활성 구독이 없습니다");
    }

    // ── processScheduledBilling ──────────────────────────────────

    @Test
    @DisplayName("정기 결제 스케줄러 - 청구 성공 시 다음 청구일 갱신")
    void processScheduledBilling_success_renewsBillingDate() {
        LocalDate today = LocalDate.now();
        Subscription sub = activeSubscription(1L, PlanType.MONTHLY_BASIC, today);

        given(subscriptionRepository.findByStatusAndNextBillingDateLessThanEqual(
                SubscriptionStatus.ACTIVE, today)).willReturn(List.of(sub));
        given(tossClient.chargeBilling(any(), any(), any(), any(), anyInt()))
                .willReturn(tossBillingResponse());
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

        paymentService.processScheduledBilling();

        assertThat(sub.getNextBillingDate()).isEqualTo(today.plusMonths(1));
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("정기 결제 스케줄러 - 청구 실패 시 구독 만료 처리")
    void processScheduledBilling_failure_expiresSubscription() {
        LocalDate today = LocalDate.now();
        Subscription sub = activeSubscription(1L, PlanType.MONTHLY_BASIC, today);

        given(subscriptionRepository.findByStatusAndNextBillingDateLessThanEqual(
                SubscriptionStatus.ACTIVE, today)).willReturn(List.of(sub));
        given(tossClient.chargeBilling(any(), any(), any(), any(), anyInt()))
                .willThrow(new RuntimeException("카드 한도 초과"));

        paymentService.processScheduledBilling();

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
    }

    // ── getHistory ───────────────────────────────────────────────

    @Test
    @DisplayName("결제 내역 조회 성공")
    void getHistory_success() {
        List<Payment> payments = List.of(
                donePayment("order-1", 1L, PlanType.PREMIUM_REPORT),
                donePayment("order-2", 1L, PlanType.MONTHLY_BASIC)
        );
        given(paymentRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(payments);

        List<PaymentHistoryResponse> result = paymentService.getHistory(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPlanType()).isEqualTo(PlanType.PREMIUM_REPORT);
        assertThat(result.get(1).getPlanType()).isEqualTo(PlanType.MONTHLY_BASIC);
    }

    @Test
    @DisplayName("결제 내역 조회 - 내역 없으면 빈 리스트 반환")
    void getHistory_empty() {
        given(paymentRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of());

        List<PaymentHistoryResponse> result = paymentService.getHistory(1L);

        assertThat(result).isEmpty();
    }
}
