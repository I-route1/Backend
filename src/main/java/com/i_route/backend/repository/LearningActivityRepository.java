package com.i_route.backend.repository;

import com.i_route.backend.entity.LearningActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningActivityRepository extends JpaRepository<LearningActivity, Long> {
    // 나중에 특정 학생의 최신 활동 내역을 조회할 때 사용합니다.
    LearningActivity findFirstByStudentIdOrderByCreatedAtDesc(String studentId);
}