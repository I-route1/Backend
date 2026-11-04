package com.i_route.backend.ai.repository;

import com.i_route.backend.ai.entity.OcrGradingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OcrGradingResultRepository extends JpaRepository<OcrGradingResult, Long> {
    Optional<OcrGradingResult> findByStudentIdAndSubmissionId(Long studentId, String submissionId);
    Optional<OcrGradingResult> findByIdAndStudentId(Long id, Long studentId);
    List<OcrGradingResult> findByStudentIdOrderByGradedAtDescIdDesc(Long studentId);
    List<OcrGradingResult> findByStudentIdAndSubjectOrderByGradedAtDescIdDesc(Long studentId, String subject);
}
