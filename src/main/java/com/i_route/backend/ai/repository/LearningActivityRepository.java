package com.i_route.backend.ai.repository;

import com.i_route.backend.ai.entity.LearningActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LearningActivityRepository extends JpaRepository<LearningActivity, Long> {
    List<LearningActivity> findByStudentIdOrderByStudyDateDesc(Long studentId);
    List<LearningActivity> findByStudentId(Long studentId);
    List<LearningActivity> findByStudentIdAndSubject(Long studentId, String subject);
    Optional<LearningActivity> findFirstByStudentIdAndInstructorFeedbackIsNotNullOrderByStudyDateDesc(Long studentId);
}