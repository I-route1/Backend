package com.i_route.backend.gps.domain.attendance.repository;

import com.i_route.backend.gps.domain.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // 가장 최근 출결 기록 (승/하차 판단용)
    Optional<Attendance> findTopByStudentIdOrderByTimestampDesc(Long studentId);

    // 특정 기간 이력 조회 (학부모용)
    List<Attendance> findByStudentIdAndTimestampBetweenOrderByTimestampDesc(
            Long studentId, LocalDateTime from, LocalDateTime to);

    // 오늘 특정 버스의 전체 출결 (관리자/교사용)
    List<Attendance> findByBusIdAndTimestampBetweenOrderByTimestampAsc(
            Long busId, LocalDateTime from, LocalDateTime to);

    // 특정 기간의 모든 출결 기록 (학원 기사용)
    List<Attendance> findByTimestampBetweenOrderByTimestampAsc(
            LocalDateTime from, LocalDateTime to);
}
