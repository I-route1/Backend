package com.i_route.backend.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "wrong_answer_entity")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WrongAnswerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long wrongAnswerId;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long questionId;

    private Integer failCount;

    private LocalDate lastFailedDate;

    @PrePersist
    private void initDefaults() {
        if (this.failCount == null) this.failCount = 1;
        if (this.lastFailedDate == null) this.lastFailedDate = LocalDate.now();
    }

    public void incrementFailCount() {
        this.failCount = (this.failCount == null ? 0 : this.failCount) + 1;
        this.lastFailedDate = LocalDate.now();
    }
}
