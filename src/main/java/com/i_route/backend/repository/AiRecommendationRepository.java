package com.i_route.backend.repository;

import com.i_route.backend.entity.AiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, Long> {
}