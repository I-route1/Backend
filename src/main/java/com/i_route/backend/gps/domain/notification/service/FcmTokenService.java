package com.i_route.backend.gps.domain.notification.service;

import com.i_route.backend.gps.domain.notification.entity.FcmToken;
import com.i_route.backend.gps.domain.notification.repository.FcmTokenRepository;
import com.i_route.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void saveToken(Long userId, String token) {

        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException(
                    "존재하지 않는 사용자입니다."
            );
        }

        String normalizedToken = token.trim();

        if (normalizedToken.isEmpty()) {
            throw new IllegalArgumentException(
                    "FCM 토큰이 비어 있습니다."
            );
        }

        fcmTokenRepository.findByToken(normalizedToken)
                .ifPresentOrElse(
                        existing -> existing.updateUserId(userId),
                        () -> fcmTokenRepository.save(
                                new FcmToken(userId, normalizedToken)
                        )
                );
    }
}
