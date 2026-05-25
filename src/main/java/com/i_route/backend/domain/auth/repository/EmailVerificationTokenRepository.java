package com.i_route.backend.domain.auth.repository;

import com.i_route.backend.domain.auth.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findByEmail(String email);

    void deleteByEmail(String email); // 재발송 시 기존 토큰 삭제
}
