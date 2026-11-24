package com.i_route.backend.gps.domain.student.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //@Column(name = "id")
    private Long studentId;

    private Long busId;

    private Long routeStopId;

    private String name;

    @JdbcTypeCode(SqlTypes.LOCAL_TIME)
    private LocalTime expectedDropOffTime;

    private Long parentId;

    private Long academyId;

    private Boolean enrollmentManaged;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "student_academy_enrollment",
            joinColumns = @JoinColumn(name = "student_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "academy_id"}),
            indexes = @Index(columnList = "academy_id"))
    @Column(name = "academy_id", nullable = false)
    private Set<Long> enrolledAcademyIds = new HashSet<>();

    @Column(unique = true)
    private String nfcCardId;

    private String gradeStudentId;

    private String grade;

    private String profileImage;

    // 학습/AI 관련 필드
    private Integer currentLevel;

    @Enumerated(EnumType.STRING)
    private LearningTendency learningTendency;

    private Double currentKoreanGrade;

    private Double studyTime;

    private String studentNote;

    private String recommendContext;

    public Set<Long> academyEnrollmentIds() {
        if (Boolean.TRUE.equals(enrollmentManaged)) {
            return Set.copyOf(enrolledAcademyIds);
        }
        return academyId == null ? Set.of() : Set.of(academyId);
    }

    public void replaceAcademyEnrollments(Set<Long> academyIds) {
        enrolledAcademyIds.clear();
        enrolledAcademyIds.addAll(academyIds);
        enrollmentManaged = true;
    }

    public enum LearningTendency {
        VISUAL, AUDITORY, KINESTHETIC
    }
}
