package com.i_route.backend.ai.repository;

import com.i_route.backend.ai.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    // 📊 기획 스펙: 학생의 과거 성적을 시간순(시험일 기준 오름차순)으로 정렬 조회
    List<Grade> findByStudentIdOrderByExamDateAsc(String studentId);
    List<Grade> findByStudentIdOrderByExamDateDesc(String studentId);
    List<Grade> findByStudentIdAndSubjectOrderByExamDateDesc(String studentId, String subject);

    @Query("SELECT AVG(g.score) FROM Grade g WHERE g.subject = :subject")
    Double getAverageScoreBySubject(@Param("subject") String subject);

    @Query("SELECT AVG(g.score) FROM Grade g WHERE g.studentId = :studentId")
    Double getAverageScoreByStudent(@Param("studentId") String studentId);

    // 표준편차 계산용 — 학생의 특정 과목 점수 목록 (날짜 오름차순)
    @Query("SELECT g.score FROM Grade g WHERE g.studentId = :studentId AND g.subject = :subject ORDER BY g.examDate ASC")
    List<Integer> findScoresByStudentAndSubject(@Param("studentId") String studentId, @Param("subject") String subject);

    // 유사 성적대 피어 분석용 — 전체 학생 평균 성적 목록
    @Query("SELECT g.studentId, AVG(g.score) FROM Grade g GROUP BY g.studentId")
    List<Object[]> findAllStudentAverageScores();

    // 선배 성공 경로용 — 특정 과목 전체 성적 (날짜 오름차순)
    List<Grade> findBySubjectOrderByExamDateAsc(String subject);

    // 위험 학생 탐지용 — 특정 학생의 과목 목록
    @Query("SELECT DISTINCT g.subject FROM Grade g WHERE g.studentId = :studentId")
    List<String> findDistinctSubjectsByStudentId(@Param("studentId") String studentId);
}