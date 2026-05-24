package com.i_route.backend.domain.ai.repository;

import com.i_route.backend.domain.ai.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<StudentEntity, Long> {
}
