package com.i_route.backend.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.i_route.backend.ai.entity.MeasurementSource;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class LearningActivityRequest {
    @NotNull @Positive
    private Long studentId;
    @NotBlank @Size(max = 255)
    private String subject;
    @NotNull
    private LocalDate studyDate;
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime studyStartTime;
    @Min(1)
    private int studyDurationMinutes;
    @Min(1) @Max(5)
    private int understandingScore;
    @Min(1) @Max(5)
    private int concentrationScore;
    @DecimalMin("0.0") @DecimalMax("1.0")
    private Double focusedRatio;
    @DecimalMin("0.0") @DecimalMax("1.0")
    private Double drowsyRatio;
    private MeasurementSource measurementSource = MeasurementSource.MANUAL;
    @Size(max = 1000)
    private String instructorFeedback;

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
