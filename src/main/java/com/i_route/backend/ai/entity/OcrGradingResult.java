package com.i_route.backend.ai.entity;

import com.i_route.backend.ai.dto.OcrResultRequest;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ocr_grading_result", uniqueConstraints = @UniqueConstraint(
        name = "uk_ocr_student_submission", columnNames = {"student_id", "submission_id"}),
        indexes = @Index(name = "idx_ocr_student_graded", columnList = "student_id,graded_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OcrGradingResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    @Column(nullable = false)
    private String subject;
    @Column(name = "submission_id", nullable = false, length = 64)
    private String submissionId;
    @Column(nullable = false)
    private int totalQuestions;
    @Column(nullable = false)
    private int correctCount;
    @Column(name = "graded_at", nullable = false, updatable = false)
    private Instant gradedAt;

    @ElementCollection
    @CollectionTable(name = "ocr_question_result", joinColumns = @JoinColumn(name = "grading_result_id"))
    @OrderColumn(name = "question_order")
    private List<OcrQuestionResult> questions = new ArrayList<>();

    public OcrGradingResult(OcrResultRequest request) {
        studentId = request.studentId();
        subject = request.subject();
        submissionId = request.submissionId();
        totalQuestions = request.totalQuestions();
        correctCount = request.correctCount();
        gradedAt = Instant.now();
        request.questions().forEach(q -> questions.add(new OcrQuestionResult(q)));
    }
}
