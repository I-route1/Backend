package com.i_route.backend.gps.domain.student.repository;

import com.i_route.backend.gps.domain.student.entity.Student;
import com.i_route.backend.student.dto.AdminStudentResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByBusId(Long busId);

    Optional<Student> findByNfcCardId(String nfcCardId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Student> findLockedByNfcCardId(String nfcCardId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Student> findLockedByStudentId(Long studentId);

    @Modifying
    @Query("update Student s set s.nfcCardId = :nfcCardId where s.studentId = :studentId")
    int updateNfcCardId(Long studentId, String nfcCardId);

    List<Student> findByParentId(Long parentId);

    List<Student> findByAcademyId(Long academyId);

    @Query("""
            select new com.i_route.backend.student.dto.AdminStudentResponse(
                s.studentId, s.name, s.grade, s.gradeStudentId)
            from Student s
            order by s.studentId
            """)
    List<AdminStudentResponse> findAdminRoster();

    @Query("""
            select distinct s.parentId from Student s
            where s.parentId is not null and
                (:academyId member of s.enrolledAcademyIds
                 or ((s.enrollmentManaged is null or s.enrollmentManaged = false) and s.academyId = :academyId))
            """)
    List<Long> findEnrolledParentIds(@Param("academyId") Long academyId);

    // 💡 parentId(학부모 유저 ID)에 해당하는 자녀 데이터 일괄 삭제 메서드 추가!
    void deleteByParentId(Long parentId);
}
