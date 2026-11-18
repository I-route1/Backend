package com.i_route.backend.gps.domain.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class AttendanceTagRequest {

    @NotNull
    @Positive
    private Long busId;

    @NotBlank
    private String nfcCardId;

    // 구버전 리더는 생략한다. 새 리더는 카드를 한 번 댈 때 생성한 UUID를 재시도에도 사용한다.
    @Pattern(regexp = "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    private String scanId;
}
