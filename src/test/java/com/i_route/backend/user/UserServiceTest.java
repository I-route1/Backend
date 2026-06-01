package com.i_route.backend.user;

import com.i_route.backend.user.dto.DuplicateCheckRequest;
import com.i_route.backend.user.dto.DuplicateCheckResponse;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.UserRepository;
import com.i_route.backend.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    // ============================================================
    // 단일 필드 중복 체크
    // ============================================================

    @Test
    @DisplayName("이메일 중복 없음")
    void checkSingleField_email_notDuplicate() {
        given(userRepository.existsByEmail("new@test.com")).willReturn(false);

        DuplicateCheckResponse resp = userService.checkSingleField("email", "new@test.com");

        assertThat(resp.isDuplicate()).isFalse();
        assertThat(resp.getMessage()).contains("사용 가능");
    }

    @Test
    @DisplayName("이메일 중복")
    void checkSingleField_email_duplicate() {
        given(userRepository.existsByEmail("taken@test.com")).willReturn(true);

        DuplicateCheckResponse resp = userService.checkSingleField("email", "taken@test.com");

        assertThat(resp.isDuplicate()).isTrue();
        assertThat(resp.getDuplicates()).contains("이메일");
    }

    @Test
    @DisplayName("닉네임 중복 없음")
    void checkSingleField_nickname_notDuplicate() {
        given(userRepository.existsByNickname("새닉네임")).willReturn(false);

        DuplicateCheckResponse resp = userService.checkSingleField("nickname", "새닉네임");

        assertThat(resp.isDuplicate()).isFalse();
    }

    @Test
    @DisplayName("닉네임 중복")
    void checkSingleField_nickname_duplicate() {
        given(userRepository.existsByNickname("기존닉네임")).willReturn(true);

        DuplicateCheckResponse resp = userService.checkSingleField("nickname", "기존닉네임");

        assertThat(resp.isDuplicate()).isTrue();
        assertThat(resp.getDuplicates()).contains("닉네임");
    }

    @Test
    @DisplayName("전화번호 중복 - 하이픈 제거 후 비교")
    void checkSingleField_phone_normalizeAndDuplicate() {
        given(userRepository.existsByPhoneNumber("01012345678")).willReturn(true);

        DuplicateCheckResponse resp = userService.checkSingleField("phone", "010-1234-5678");

        assertThat(resp.isDuplicate()).isTrue();
        assertThat(resp.getDuplicates()).contains("휴대폰 번호");
    }

    @Test
    @DisplayName("잘못된 타입 - null 반환")
    void checkSingleField_invalidType_returnsNull() {
        DuplicateCheckResponse resp = userService.checkSingleField("unknown", "value");
        assertThat(resp).isNull();
    }

    // ============================================================
    // 복합 중복 체크
    // ============================================================

    @Test
    @DisplayName("중복 없음")
    void checkDuplicate_noDuplicate() {
        given(userRepository.findByEmailOrUsernameOrNicknameOrPhoneNumber(any(), any(), any(), any()))
                .willReturn(Optional.empty());

        DuplicateCheckRequest req = new DuplicateCheckRequest();
        req.setUsername("new@test.com");
        req.setNickname("새닉");
        req.setPhoneNumber("01099999999");

        DuplicateCheckResponse resp = userService.checkDuplicate(req);

        assertThat(resp.isDuplicate()).isFalse();
        assertThat(resp.getMessage()).contains("사용 가능");
    }

    @Test
    @DisplayName("이메일과 닉네임 동시 중복")
    void checkDuplicate_emailAndNicknameDuplicate() {
        User existing = User.builder()
                .email("dup@test.com").nickname("중복닉").phoneNumber("01011111111")
                .role(User.UserRole.PARENT).loginType(User.LoginType.EMAIL).build();

        given(userRepository.findByEmailOrUsernameOrNicknameOrPhoneNumber(any(), any(), any(), any()))
                .willReturn(Optional.of(existing));

        DuplicateCheckRequest req = new DuplicateCheckRequest();
        req.setUsername("dup@test.com");
        req.setNickname("중복닉");
        req.setPhoneNumber("01099999999");

        DuplicateCheckResponse resp = userService.checkDuplicate(req);

        assertThat(resp.isDuplicate()).isTrue();
        assertThat(resp.getDuplicates()).containsExactlyInAnyOrder("이메일", "닉네임");
        assertThat(resp.getMessage()).contains("이메일").contains("닉네임");
    }

    @Test
    @DisplayName("전화번호 하이픈 포함 시 정규화 후 중복 판정")
    void checkDuplicate_phoneNormalization() {
        User existing = User.builder()
                .email("other@test.com").nickname("다른닉").phoneNumber("01012345678")
                .role(User.UserRole.PARENT).loginType(User.LoginType.EMAIL).build();

        given(userRepository.findByEmailOrUsernameOrNicknameOrPhoneNumber(any(), any(), any(), any()))
                .willReturn(Optional.of(existing));

        DuplicateCheckRequest req = new DuplicateCheckRequest();
        req.setUsername("new@test.com");
        req.setNickname("새닉");
        req.setPhoneNumber("010-1234-5678");

        DuplicateCheckResponse resp = userService.checkDuplicate(req);

        assertThat(resp.isDuplicate()).isTrue();
        assertThat(resp.getDuplicates()).contains("휴대폰 번호");
    }
}
