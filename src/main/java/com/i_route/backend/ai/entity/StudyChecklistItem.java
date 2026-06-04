package com.i_route.backend.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentId;

    private String subject;

    @Column(nullable = false)
    private String task;

    private int estimatedMinutes;

    @Builder.Default
    private boolean done = false;

    private LocalDate planDate;

    public void toggleDone() {
        this.done = !this.done;
    }
}
