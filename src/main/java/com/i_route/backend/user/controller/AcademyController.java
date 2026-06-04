package com.i_route.backend.user.controller;

import com.i_route.backend.user.entity.Academy;
import com.i_route.backend.user.repository.AcademyRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/academies")
@RequiredArgsConstructor
public class AcademyController {

    private final AcademyRepository academyRepository;

    @GetMapping
    public List<AcademyResponse> getAcademies() {
        return academyRepository.findAll().stream()
                .filter(a -> a.getAcademyName() != null && a.getInviteCode() != null)
                .map(a -> AcademyResponse.builder()
                        .academyId(a.getAcademyId())
                        .academyName(a.getAcademyName())
                        .academyAddress(a.getAcademyAddress())
                        .inviteCode(a.getInviteCode())
                        .build())
                .collect(Collectors.toList());
    }

    @Getter
    @Builder
    static class AcademyResponse {
        private Long academyId;
        private String academyName;
        private String academyAddress;
        private String inviteCode;
    }
}
