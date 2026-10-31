package com.i_route.backend.user.controller;

import com.i_route.backend.global.security.CustomUserDetails;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

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
}