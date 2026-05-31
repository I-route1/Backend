package com.i_route.backend.gps.domain.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class GradeStudentIdRequest {

    @NotBlank
    private String gradeStudentId;
}
