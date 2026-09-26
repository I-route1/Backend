package com.i_route.backend.ai.repository;

import com.i_route.backend.ai.entity.WrongAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WrongAnswerRepository extends JpaRepository<WrongAnswer, Long> {

    // 특정 학생의 특정 개념 오답 이력 조회
    List<WrongAnswer> findByStudentIdAndConceptTag(Long studentId, String conceptTag);

    // 학생의 전체 오답 (프리미엄 리포트의 주 취약 과목 선정용)
    List<WrongAnswer> findByStudentId(Long studentId);

    // 이미 틀렸던 문항인지 체크
    Optional<WrongAnswer> findByStudentIdAndQuestionId(Long studentId, String questionId);

    @org.springframework.data.jpa.repository.Query(
            "SELECT w FROM WrongAnswer w WHERE w.studentId = :studentId AND w.subject = :subject ORDER BY w.failCount DESC"
    )
    List<WrongAnswer> findTopWeaknessByStudentIdAndSubject(
            @org.springframework.data.repository.query.Param("studentId") Long studentId,
            @org.springframework.data.repository.query.Param("subject") String subject
    );
}