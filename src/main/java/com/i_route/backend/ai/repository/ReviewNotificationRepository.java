package com.i_route.backend.ai.repository;

import com.i_route.backend.ai.entity.ReviewNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewNotificationRepository extends JpaRepository<ReviewNotification, Long> {

    List<ReviewNotification> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<ReviewNotification> findByStudentIdAndIsReadFalseOrderByCreatedAtDesc(Long studentId);
}
