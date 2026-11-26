package com.i_route.backend.auth;

import com.i_route.backend.auth.entity.RefreshToken;
import com.i_route.backend.auth.repository.RefreshTokenRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RefreshTokenRepositoryTest {
    @Autowired
    private RefreshTokenRepository tokens;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAndReadsRefreshTokenLongerThan255Characters() {
        String token = "sample-refresh-token.".repeat(20);
        tokens.saveAndFlush(RefreshToken.builder()
                .userId(1L)
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build());
        entityManager.clear();

        assertThat(tokens.findByToken(token)).get()
                .satisfies(saved -> assertThat(saved.getToken()).isEqualTo(token));
    }
}
