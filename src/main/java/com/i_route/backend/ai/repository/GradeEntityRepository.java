package com.i_route.backend.ai.repository;

import com.i_route.backend.ai.entity.GradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GradeEntityRepository extends JpaRepository<GradeEntity, Long> {

    List<GradeEntity> findByStudentIdOrderByTestDateAsc(Long studentId);

    List<GradeEntity> findByStudentIdAndSubjectIdOrderByTestDateAsc(Long studentId, Long subjectId);
}
