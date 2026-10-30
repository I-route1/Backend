package com.i_route.backend.auth.service;

import com.i_route.backend.auth.dto.KakaoUserInfoResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class KakaoOAuthService {

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;


    @Value("${kakao.admin-key}")
    private String adminKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getAccessToken(String code) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> params =
                new LinkedMultiValueMap<>();

        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        System.out.println("========== KAKAO TOKEN REQUEST ==========");
        System.out.println("clientId = " + clientId);
        System.out.println("redirectUri = " + redirectUri);
        System.out.println("code = " + code);
        System.out.println("params = " + params);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(params, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(
                        "https://kauth.kakao.com/oauth/token",
                        HttpMethod.POST,
                        request,
                        Map.class
                );

        System.out.println("response = " + response.getBody());

        return (String) response.getBody().get("access_token");
    }

    public KakaoUserInfoResponse getUserInfo(String accessToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request =
                new HttpEntity<>(headers);

        ResponseEntity<KakaoUserInfoResponse> response =
                restTemplate.exchange(
                        "https://kapi.kakao.com/v2/user/me",
                        HttpMethod.GET,
                        request,
                        KakaoUserInfoResponse.class
                );

        return response.getBody();
    }

    // KakaoOAuthService.java 내부 연동 해제 메서드 점검
    public void unlinkKakaoUser(Long kakaoId) {
        if (kakaoId == null) return;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        // ⚠️ Content-Type이 application/x-www-form-urlencoded 인지 확인
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        // ⚠️ Authorization 헤더 형식이 KakaoAK {ADMIN_KEY} 인지 확인 (공백 중요)
        headers.set("Authorization", "KakaoAK " + adminKey);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("target_id_type", "user_id");
        params.add("target_id", String.valueOf(kakaoId));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://kapi.kakao.com/v1/user/unlink",
                    request,
                    String.class
            );
            System.out.println("카카오 연동 해제 성공: " + response.getBody());
        } catch (Exception e) {
            System.err.println("카카오 연동 해제 실패 (kakaoId: " + kakaoId + "): " + e.getMessage());
        }
    }
}