package com.i_route.backend.ai;

import com.i_route.backend.auth.dto.LoginRequest;
import com.i_route.backend.auth.entity.RefreshToken;
import com.i_route.backend.auth.repository.RefreshTokenRepository;
import com.i_route.backend.auth.service.AuthService;
import com.i_route.backend.global.jwt.JwtUtil;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthRefreshEdgeTest {
    @Mock UserRepository users;
    @Mock RefreshTokenRepository refreshTokens;
    @Mock PasswordEncoder encoder;
    @Spy JwtUtil jwt = new JwtUtil();
    @InjectMocks AuthService auth;
    User user;
    @BeforeEach void setup() {
        ReflectionTestUtils.setField(jwt, "secret", "local-test-signing-key-only-not-a-real-secret-12345678901234567890");
        ReflectionTestUtils.setField(jwt, "expiration", 60000L);
        ReflectionTestUtils.setField(jwt, "refreshExpiration", 120000L);
        ReflectionTestUtils.setField(auth, "refreshExpiration", 120000L);
        user = User.builder().id(42L).username("fixture").nickname("fixture").role(User.UserRole.PARENT).build();
    }
    @Test void loginPersistsRefreshTokenAndItCanReissueAccessToken() {
        when(users.findByUsername("fixture")).thenReturn(Optional.of(user));
        when(encoder.matches("password", null)).thenReturn(true);
        var request = new LoginRequest(); request.setUsername("fixture"); request.setPassword("password");
        var login = auth.login(request);
        var saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens).save(saved.capture());
        assertThat(saved.getValue().getToken()).isEqualTo(login.getRefreshToken());
        when(refreshTokens.findByToken(login.getRefreshToken())).thenReturn(Optional.of(saved.getValue()));
        when(users.findById(42L)).thenReturn(Optional.of(user));
        assertThat(jwt.validateToken(auth.reissueAccessToken(login.getRefreshToken()).getAccessToken())).isTrue();
        assertThatThrownBy(() -> jwt.validateToken(login.getRefreshToken())).isInstanceOf(RuntimeException.class);
        assertThat(jwt.generateRefreshToken(42L)).isNotEqualTo(jwt.generateRefreshToken(42L));
    }
    @Test void missingUnknownAndExpiredRefreshTokensReturn401() {
        for (String token : new String[] {null, "", "unknown"}) {
            assertThatThrownBy(() -> auth.reissueAccessToken(token)).isInstanceOfSatisfying(ResponseStatusException.class,
                    e -> assertThat(e.getStatusCode().value()).isEqualTo(401));
        }
        String token = jwt.generateRefreshToken(42L);
        when(refreshTokens.findByToken(token)).thenReturn(Optional.of(RefreshToken.builder().token(token).userId(42L)
                .expiresAt(LocalDateTime.now().minusSeconds(1)).build()));
        assertThatThrownBy(() -> auth.reissueAccessToken(token)).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(401));
    }
}
