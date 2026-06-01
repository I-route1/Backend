package com.i_route.backend.auth.service;

import com.i_route.backend.auth.entity.EmailVerificationToken;
import com.i_route.backend.auth.repository.EmailVerificationTokenRepository;
import com.i_route.backend.global.response.ApiResponse;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailService {

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final UserRepository userRepository;

    public ApiResponse<String> verifyEmail(String token) {

        EmailVerificationToken entity =
                emailVerificationTokenRepository.findByToken(token)
                        .orElse(null);

        if (entity == null) {
            return ApiResponse.fail("INVALID_TOKEN", "유효하지 않은 토큰입니다.");
        }

        if (Boolean.TRUE.equals(entity.isVerified())) {
            return ApiResponse.success("이미 인증된 이메일입니다.");
        }

        if (entity.getExpiryDate() != null &&
                entity.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ApiResponse.fail("EXPIRED_TOKEN", "만료된 토큰입니다.");
        }

        entity.setVerified(true);

        emailVerificationTokenRepository.save(entity);

        return ApiResponse.success("이메일 인증 완료");
    }
}
