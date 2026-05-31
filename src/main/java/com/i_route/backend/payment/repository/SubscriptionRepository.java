package com.i_route.backend.payment.repository;

import com.i_route.backend.payment.entity.Subscription;
import com.i_route.backend.payment.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    // 정기 결제 스케줄러: 오늘 청구일인 활성 구독 전체 조회
    List<Subscription> findByStatusAndNextBillingDateLessThanEqual(
            SubscriptionStatus status, LocalDate date);
}
