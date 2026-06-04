package com.i_route.backend.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class ChildCreateRequest {

    @NotBlank
    private String name;

    private String grade;

    private List<AcademyInfo> academies;

    @Getter
    @NoArgsConstructor
    public static class AcademyInfo {
        private String code;
        private String name;
    }
}
