package com.i_route.backend.domain.ai.repository;

import com.i_route.backend.domain.ai.entity.LearningActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LearningActivityRepository extends JpaRepository<LearningActivity, Long> {
    // 🌟 특정 학생의 학습 기록을 '날짜 최신순'으로 불러오는 메서드
    List<LearningActivity> findByStudentIdOrderByStudyDateDesc(String studentId);
    List<LearningActivity> findByStudentIdAndSubject(String studentId, String subject);
}