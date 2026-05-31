package com.i_route.backend.gps.domain.student.repository;

import com.i_route.backend.gps.domain.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByBusId(Long busId);

    Optional<Student> findByNfcCardId(String nfcCardId);
}
