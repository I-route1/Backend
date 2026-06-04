package com.i_route.backend.ai.repository;

import com.i_route.backend.ai.entity.StudyChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StudyChecklistRepository extends JpaRepository<StudyChecklistItem, Long> {
    List<StudyChecklistItem> findByStudentIdAndPlanDateOrderByIdAsc(Long studentId, LocalDate planDate);
    List<StudyChecklistItem> findByStudentIdOrderByPlanDateDescIdAsc(Long studentId);
}
