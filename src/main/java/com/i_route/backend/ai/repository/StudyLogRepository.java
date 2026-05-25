package com.i_route.backend.ai.repository;

import com.i_route.backend.ai.entity.StudyLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudyLogRepository extends JpaRepository<StudyLogEntity, Long> {

    List<StudyLogEntity> findByStudentId(Long studentId);

    List<StudyLogEntity> findByStudentIdOrderByStartTimeDesc(Long studentId);
}
