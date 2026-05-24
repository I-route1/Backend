package com.i_route.backend.ai.repository;

import com.i_route.backend.ai.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByConceptTag(String conceptTag);

    List<Question> findBySubjectIdAndDifficulty(Long subjectId, Integer difficulty);
}
