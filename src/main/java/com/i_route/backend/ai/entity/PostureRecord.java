package com.i_route.backend.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "posture_record", uniqueConstraints = @UniqueConstraint(name = "uk_posture_student_submission",
        columnNames = {"student_id", "submission_id"}),
        indexes = @Index(name = "idx_posture_student_started", columnList = "student_id,started_at"))
@Getter @Builder @AllArgsConstructor @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostureRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    @Column(name = "submission_id", nullable = false, length = 64)
    private String submissionId;
    @Column(nullable = false)
    private String subject;
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private int durationSeconds;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12)
    private PostureInputSource inputSource;
    @Column(nullable = false, length = 64)
    private String modelName;
    private int totalSamples;
    private int uprightSamples;
    private int slouchedSamples;
    private int headDownSamples;
    private int tiltedSamples;
    private int unknownSamples;
    private int noPersonSamples;
    private int multiplePeopleSamples;
}
