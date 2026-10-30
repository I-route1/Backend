package com.i_route.backend.gps.domain.attendance.dto;

import com.i_route.backend.gps.domain.student.entity.Student;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChildResponse {
    private Long gpsStudentId;
    private String gradeStudentId;
    private String name;
    private Long busId;

    public static ChildResponse from(Student student) {
        return ChildResponse.builder()
                .gpsStudentId(student.getStudentId())
                .gradeStudentId(student.getGradeStudentId())
                .name(student.getName())
                .busId(student.getBusId())
                .build();
    }
}
