package com.i_route.backend.ai.repository;

import com.i_route.backend.ai.entity.AiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, Long> {

    List<AiRecommendation> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<AiRecommendation> findByStudentIdOrderByCreatedAtDesc(String studentId);
    List<AiRecommendation> findByStudentIdAndCreatedAtBetween(String studentId, LocalDateTime start, LocalDateTime end);
}