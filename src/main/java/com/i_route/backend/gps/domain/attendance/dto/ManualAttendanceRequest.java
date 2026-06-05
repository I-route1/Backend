package com.i_route.backend.gps.domain.attendance.dto;

import com.i_route.backend.gps.domain.attendance.entity.AttendanceEventType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ManualAttendanceRequest {

    @NotNull
    private Long studentId;

    @NotNull
    private AttendanceEventType eventType;
}
