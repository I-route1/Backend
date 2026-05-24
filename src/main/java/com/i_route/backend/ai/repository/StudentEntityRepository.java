package com.i_route.backend.ai.repository;

import com.i_route.backend.ai.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentEntityRepository extends JpaRepository<StudentEntity, Long> {
}
