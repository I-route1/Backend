package com.i_route.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentId;

    @Column(columnDefinition = "TEXT")
    private String recommendedContext; // AI가 찾아준 족보 내용

    @Builder
    public AiRecommendation(String studentId, String recommendedContext) {
        this.studentId = studentId;
        this.recommendedContext = recommendedContext;
    }
}