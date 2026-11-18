package com.i_route.backend.gps.domain.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance", uniqueConstraints = @UniqueConstraint(
        name = "uk_attendance_student_scan", columnNames = {"student_id", "scan_id"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long busId;

    @Column(name = "scan_id", length = 36)
    private String scanId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceEventType eventType;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
