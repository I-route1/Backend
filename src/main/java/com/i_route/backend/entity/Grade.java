package com.i_route.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentId; // 학생 식별자
    private int score;        // 획득 점수
    private double average;   // 당시 전체 평균
    private double stdDev;    // 당시 표준편차
    private String weakTag;   // 취약 개념 태그

    private LocalDateTime createdAt; // 성적 입력 시간

    @Builder
    public Grade(String studentId, int score, double average, double stdDev, String weakTag) {
        this.studentId = studentId;
        this.score = score;
        this.average = average;
        this.stdDev = stdDev;
        this.weakTag = weakTag;
        this.createdAt = LocalDateTime.now();
    }
}