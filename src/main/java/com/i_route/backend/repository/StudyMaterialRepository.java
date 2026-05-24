package com.i_route.backend.repository;

import com.i_route.backend.entity.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {

    List<StudyMaterial> findByLevelLessThanEqualOrderByLevelDesc(Integer level);

    List<StudyMaterial> findBySubjectId(Long subjectId);
}
