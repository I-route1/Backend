package com.i_route.backend.repository;

import com.i_route.backend.entity.TargetGoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TargetGoalRepository extends JpaRepository<TargetGoalEntity, Long> {

    Optional<TargetGoalEntity> findByStudentId(Long studentId);
}
