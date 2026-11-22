package com.i_route.backend.user.controller;

import com.i_route.backend.global.security.CustomUserDetails;
import com.i_route.backend.user.dto.UserProfileResponse;
import com.i_route.backend.user.dto.UserProfileUpdateRequest;
import com.i_route.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping("/me/credits")
    public ResponseEntity<Map<String, Integer>> getCredits(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(Map.of("premiumCredits", user.getPremiumCredits()));
    }

    @PostMapping("/me/credits/use")
    public ResponseEntity<Map<String, Integer>> useCredit(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (user.getPremiumCredits() <= 0) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "크레딧이 부족합니다.");
        }
        user.setPremiumCredits(user.getPremiumCredits() - 1);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("premiumCredits", user.getPremiumCredits()));
    }

    // GET /api/users/me -> 내 프로필 데이터 반환 (이메일, 전화번호 포함)
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UserProfileResponse response = userService.getMyProfile(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/profile-card")
    public ProfileCard getProfileCard(@PathVariable Long id,
                                      @AuthenticationPrincipal CustomUserDetails viewer) {
        if (viewer == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String email = user.isEmailPublic() ? user.getEmail() : null;
        String phoneNumber = user.isPhoneNumberPublic() ? user.getPhoneNumber() : null;
        return new ProfileCard(user.getNickname(), user.getProfileImageUrl(), email, phoneNumber);
    }

    public record ProfileCard(String nickname, String profileImageUrl, String email, String phoneNumber) {}

    @PatchMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserProfileUpdateRequest request
    ) {
        UserProfileResponse updatedProfile = userService.updateMyProfile(userDetails.getId(), request);
        return ResponseEntity.ok(updatedProfile);
    }

    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileResponse> updateMyProfileWithImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("data") UserProfileUpdateRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        UserProfileResponse updatedProfile = userService.updateMyProfile(userDetails.getId(), request, profileImage);
        return ResponseEntity.ok(updatedProfile);
    }

    @GetMapping("/check-duplicate")
    public ResponseEntity<com.i_route.backend.user.dto.DuplicateCheckResponse> checkSingleField(
            @RequestParam("type") String type,
            @RequestParam("value") String value
    ) {
        return ResponseEntity.ok(userService.checkSingleField(type, value));
    }
}