package com.i_route.backend.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.i_route.backend.ai.entity.MeasurementSource;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class LearningActivityRequest {
    @NotNull @Positive
    @JsonDeserialize(using = ExactStudentIdDeserializer.class)
    private Long studentId;
    @Size(max = 64) @Pattern(regexp = ".*\\S.*", message = "submissionId는 공백일 수 없습니다.")
    private String submissionId;
    @NotBlank @Size(max = 255)
    private String subject;
    @NotNull
    private LocalDate studyDate;
    private LocalTime studyStartTime;
    @Min(1) @Max(1440)
    @JsonDeserialize(using = ExactIntegerDeserializer.class)
    private int studyDurationMinutes;
    @Min(1) @Max(5)
    @JsonDeserialize(using = ExactIntegerDeserializer.class)
    private int understandingScore;
    @Min(1) @Max(5)
    @JsonDeserialize(using = ExactIntegerDeserializer.class)
    private int concentrationScore;
    @DecimalMin("0.0") @DecimalMax("1.0")
    private Double focusedRatio;
    @DecimalMin("0.0") @DecimalMax("1.0")
    private Double drowsyRatio;
    private MeasurementSource measurementSource = MeasurementSource.MANUAL;
    @Size(max = 1000)
    private String instructorFeedback;

    @AssertTrue(message = "학습일은 1000-01-01부터 9999-12-31까지 입력할 수 있습니다.")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isStudyDateSupported() {
        return studyDate == null || (studyDate.getYear() >= 1000 && studyDate.getYear() <= 9999);
    }

    @AssertTrue(message = "카메라 측정은 집중·졸음 비율이 필요하며 비율의 합은 1 이하여야 합니다.")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isMeasurementValid() {
        if (measurementSource == MeasurementSource.CAMERA && (focusedRatio == null || drowsyRatio == null)) {
            return false;
        }
        return (focusedRatio == null || Double.isFinite(focusedRatio))
                && (drowsyRatio == null || Double.isFinite(drowsyRatio))
                && (focusedRatio == null || drowsyRatio == null || focusedRatio + drowsyRatio <= 1.0 + 1e-9);
    }
}
