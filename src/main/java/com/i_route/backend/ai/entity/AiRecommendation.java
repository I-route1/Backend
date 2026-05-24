package com.i_route.backend.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder // 🔥 클래스 레벨 빌더
@AllArgsConstructor // 🔥 빌더 짝꿍 추가!
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String careerAnalysis;

    @Column(columnDefinition = "TEXT")
    private String learningGuide;

    private LocalDateTime createdAt;
}