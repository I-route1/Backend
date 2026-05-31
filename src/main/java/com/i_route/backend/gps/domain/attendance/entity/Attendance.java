package com.i_route.backend.gps.domain.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceEventType eventType;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
