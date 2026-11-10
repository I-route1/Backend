package com.i_route.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.i_route.backend.ai.entity.PostureInputSource;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Getter @Setter
public class PostureRecordRequest {
    @NotNull @Positive @JsonDeserialize(using = ExactStudentIdDeserializer.class)
    private Long studentId;
    @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,63}")
    @JsonDeserialize(using = ExactTextDeserializer.class)
    private String submissionId;
    @NotBlank @Size(max = 255)
    @JsonDeserialize(using = ExactTextDeserializer.class)
    private String subject;
    @NotNull @JsonDeserialize(using = ExactOffsetDateTimeDeserializer.class)
    private OffsetDateTime startedAt;
    @NotNull @Min(1) @Max(86400) @JsonDeserialize(using = ExactIntegerDeserializer.class)
    private Integer durationSeconds;
    @NotNull
    private PostureInputSource inputSource;
    @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,63}")
    @JsonDeserialize(using = ExactTextDeserializer.class)
    private String modelName;
    @NotNull @Min(1) @Max(86400) @JsonDeserialize(using = ExactIntegerDeserializer.class)
    private Integer totalSamples;
    @NotNull @Min(0) @Max(86400) @JsonDeserialize(using = ExactIntegerDeserializer.class)
    private Integer uprightSamples;
    @NotNull @Min(0) @Max(86400) @JsonDeserialize(using = ExactIntegerDeserializer.class)
    private Integer slouchedSamples;
    @NotNull @Min(0) @Max(86400) @JsonDeserialize(using = ExactIntegerDeserializer.class)
    private Integer headDownSamples;
    @NotNull @Min(0) @Max(86400) @JsonDeserialize(using = ExactIntegerDeserializer.class)
    private Integer tiltedSamples;
    @NotNull @Min(0) @Max(86400) @JsonDeserialize(using = ExactIntegerDeserializer.class)
    private Integer unknownSamples;
    @NotNull @Min(0) @Max(86400) @JsonDeserialize(using = ExactIntegerDeserializer.class)
    private Integer noPersonSamples;
    @NotNull @Min(0) @Max(86400) @JsonDeserialize(using = ExactIntegerDeserializer.class)
    private Integer multiplePeopleSamples;

    @JsonIgnore @AssertTrue(message = "과목에는 보이는 문자가 포함되어야 합니다.")
    public boolean isSubjectVisible() {
        return subject == null || hasVisibleSubject(subject);
    }

    public static boolean hasVisibleSubject(String value) {
        return value != null && (value.codePoints().noneMatch(c -> Character.getType(c) == Character.SURROGATE)
                && value.codePoints().anyMatch(c -> !Character.isWhitespace(c) && !Character.isSpaceChar(c)
                && !Character.isISOControl(c) && Character.getType(c) != Character.FORMAT));
    }

    @JsonIgnore @AssertTrue(message = "자세별 표본 수의 합이 전체 표본 수와 일치해야 합니다.")
    public boolean isSampleCountValid() {
        Integer[] values = {uprightSamples, slouchedSamples, headDownSamples, tiltedSamples,
                unknownSamples, noPersonSamples, multiplePeopleSamples};
        long sum = 0;
        for (var value : values) {
            if (value == null) return true; // @NotNull reports the missing field.
            sum += value;
        }
        return totalSamples == null || sum == totalSamples;
    }

    @JsonIgnore @AssertTrue(message = "측정 시각의 연도는 1000~9999 범위여야 합니다.")
    public boolean isStartedAtSupported() {
        if (startedAt == null) return true;
        if (startedAt.getYear() < 1000 || startedAt.getYear() > 9999) return false;
        int year = startedAt.withOffsetSameInstant(java.time.ZoneOffset.UTC).getYear();
        return year >= 1000 && year <= 9999;
    }
}
