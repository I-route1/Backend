package com.i_route.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "grade_entity")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GradeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long gradeId;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long subjectId;

    private Integer score;

    private LocalDate testDate;

    private Double percentile;

    public void updateScore(Integer score) {
        this.score = score;
    }
}
