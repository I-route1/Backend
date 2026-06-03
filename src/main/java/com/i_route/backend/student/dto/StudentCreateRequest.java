package com.i_route.backend.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StudentCreateRequest {

    @NotBlank
    private String name;

    private String gradeStudentId;
}
