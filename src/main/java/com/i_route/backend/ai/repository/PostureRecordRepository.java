package com.i_route.backend.ai.repository;

import com.i_route.backend.ai.entity.PostureRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PostureRecordRepository extends JpaRepository<PostureRecord, Long> {
    Optional<PostureRecord> findByStudentIdAndSubmissionId(Long studentId, String submissionId);
    List<PostureRecord> findByStudentIdOrderByStartedAtDescIdDesc(Long studentId, Pageable page);
    List<PostureRecord> findByStudentIdAndSubjectOrderByStartedAtDescIdDesc(Long studentId, String subject, Pageable page);
}
