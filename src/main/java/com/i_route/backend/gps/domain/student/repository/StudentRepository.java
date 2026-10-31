package com.i_route.backend.gps.domain.student.repository;

import com.i_route.backend.gps.domain.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByBusId(Long busId);

    Optional<Student> findByNfcCardId(String nfcCardId);

    List<Student> findByParentId(Long parentId);

    List<Student> findByAcademyId(Long academyId);

    // 💡 parentId(학부모 유저 ID)에 해당하는 자녀 데이터 일괄 삭제 메서드 추가!
    void deleteByParentId(Long parentId);
}
