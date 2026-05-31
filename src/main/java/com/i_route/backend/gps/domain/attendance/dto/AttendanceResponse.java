package com.i_route.backend.gps.domain.attendance.dto;

import com.i_route.backend.gps.domain.attendance.entity.AttendanceEventType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AttendanceResponse {

    private Long attendanceId;
    private Long studentId;
    private String studentName;
    private Long busId;
    private AttendanceEventType eventType;
    private LocalDateTime timestamp;
}
