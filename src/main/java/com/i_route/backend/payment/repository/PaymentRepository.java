package com.i_route.backend.payment.repository;

import com.i_route.backend.payment.entity.Payment;
import com.i_route.backend.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    List<Payment> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, PaymentStatus status);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
}
