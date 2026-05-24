package com.i_route.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_log")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long subjectId;

    private Integer durationMinutes;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
