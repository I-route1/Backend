package com.i_route.backend.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_info")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentInfo {

    @Id
    @Column(name = "student_id")
    private String studentId;

    @Column(name = "current_korean_grade")
    private Double currentKoreanGrade;

    @Column(name = "study_time")
    private Double studyTime;

    @Column(name = "student_note", columnDefinition = "TEXT")
    private String studentNote;

    @Column(name = "recommend_context", columnDefinition = "TEXT")
    private String recommendContext;
}