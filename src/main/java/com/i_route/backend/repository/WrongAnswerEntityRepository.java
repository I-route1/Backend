package com.i_route.backend.repository;

import com.i_route.backend.entity.WrongAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WrongAnswerEntityRepository extends JpaRepository<WrongAnswerEntity, Long> {

    List<WrongAnswerEntity> findByStudentIdOrderByFailCountDesc(Long studentId);
}
