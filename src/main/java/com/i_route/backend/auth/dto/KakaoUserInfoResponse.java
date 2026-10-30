package com.i_route.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
public class KakaoUserInfoResponse {

    private Long id;
    private String nickname;
    private String email;
    private String phoneNumber;

    @JsonProperty("kakao_account")
    private void unpackKakaoAccount(Map<String, Object> kakaoAccount) {
        if (kakaoAccount == null) return;

        // 이메일 추출
        if (kakaoAccount.containsKey("email")) {
            this.email = (String) kakaoAccount.get("email");
        }

        // 전화번호 추출 (+82 10-1234-5678 형식을 01012345678로 변환)
        if (kakaoAccount.containsKey("phone_number")) {
            String rawPhone = (String) kakaoAccount.get("phone_number");
            if (rawPhone != null) {
                this.phoneNumber = rawPhone.replace("+82 ", "0").replaceAll("[\\s-]", "");
            }
        }

        // 프로필 닉네임 추출
        if (kakaoAccount.containsKey("profile")) {
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            if (profile != null && profile.containsKey("nickname")) {
                this.nickname = (String) profile.get("nickname");
            }
        }
    }
}