package com.i_route.backend.student.dto;

import com.i_route.backend.gps.domain.student.entity.Student;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StudentResponse {

    private Long studentId;
    private String name;
    private String grade;
    private String gradeStudentId;
    private Long parentId;
    private Long busId;
    private Long routeStopId;
    private Long academyId;
    private String academyName;
    private List<AcademyInfo> academies;

    @Getter
    @Builder
    public static class AcademyInfo {
        private Long id;
        private String name;
        private String code;
    }

    public static StudentResponse from(Student student) {
        return StudentResponse.builder()
                .studentId(student.getStudentId())
                .name(student.getName())
                .grade(student.getGrade())
                .gradeStudentId(student.getGradeStudentId())
                .parentId(student.getParentId())
                .busId(student.getBusId())
                .routeStopId(student.getRouteStopId())
                .academyId(student.getAcademyId())
                .academies(List.of())
                .build();
    }

    public static StudentResponse from(Student student, String academyName, String academyCode) {
        return StudentResponse.builder()
                .studentId(student.getStudentId())
                .name(student.getName())
                .grade(student.getGrade())
                .gradeStudentId(student.getGradeStudentId())
                .parentId(student.getParentId())
                .busId(student.getBusId())
                .routeStopId(student.getRouteStopId())
                .academyId(student.getAcademyId())
                .academyName(academyName)
                .academies(academyName != null ? List.of(
                    AcademyInfo.builder()
                        .id(student.getAcademyId())
                        .name(academyName)
                        .code(academyCode)
                        .build()
                ) : List.of())
                .build();
    }
}
