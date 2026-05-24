package com.i_route.backend.domain.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "wrong_answer") // DB에 생성될 테이블 이름
public class WrongAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String subject;

    @Column(name = "question_id", nullable = false)
    private String questionId;

    @Column(name = "concept_tag", nullable = false)
    private String conceptTag;

    @Column(name = "fail_count")
    private int failCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.failCount = 1;
    }
}