package com.i_route.backend.gps.domain.gps.dto.response;

import com.i_route.backend.gps.domain.student.entity.PassengerStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PassengerResponse {

    private Long studentId;

    private String name;

    private String profileImage;

    private String stopName;

    private PassengerStatus status;

    private Long academyId;

    private String academyName;
}
