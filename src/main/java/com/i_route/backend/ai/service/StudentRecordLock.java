package com.i_route.backend.ai.service;

import com.i_route.backend.gps.domain.student.entity.Student;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** 학생 행의 DB 잠금으로 여러 서버/트랜잭션의 중복 저장과 오답 횟수 갱신을 직렬화한다. */
@Component
@RequiredArgsConstructor
public class StudentRecordLock {
    private final EntityManager em;
    public void lock(Long studentId) {
        if (studentId == null || studentId <= 0) throw new IllegalArgumentException("studentId는 양수여야 합니다.");
        if (em.find(Student.class, studentId, LockModeType.PESSIMISTIC_WRITE) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "학생을 찾을 수 없습니다.");
        }
    }
}
