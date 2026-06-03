package com.i_route.backend.student.dto;

import com.i_route.backend.gps.domain.student.entity.Student;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentResponse {

    private Long studentId;
    private String name;
    private String gradeStudentId;
    private Long parentId;
    private Long busId;
    private Long routeStopId;

    public static StudentResponse from(Student student) {
        return StudentResponse.builder()
                .studentId(student.getStudentId())
                .name(student.getName())
                .gradeStudentId(student.getGradeStudentId())
                .parentId(student.getParentId())
                .busId(student.getBusId())
                .routeStopId(student.getRouteStopId())
                .build();
    }
}
