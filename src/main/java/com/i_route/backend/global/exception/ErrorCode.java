package com.i_route.backend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    BUS_NOT_FOUND(HttpStatus.NOT_FOUND, "BUS_001", "차량을 찾을 수 없습니다."),
    BUS_NOT_OPERATING(HttpStatus.BAD_REQUEST, "BUS_002", "운행 시간이 아니므로 위치를 조회할 수 없습니다."),

    CURRENT_LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "GPS_001", "현재 차량 위치가 없습니다."),
    INVALID_GPS_LOCATION(HttpStatus.BAD_REQUEST, "GPS_002", "비정상 GPS 데이터입니다."),
    KAKAO_API_ERROR(HttpStatus.BAD_GATEWAY, "GPS_003", "카카오 길찾기 API 호출에 실패했습니다."),
    KAKAO_ROUTE_NOT_FOUND(HttpStatus.BAD_GATEWAY,"GPS_004","카카오 길찾기 경로를 찾을 수 없습니다."),

    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "ROUTE_001", "노선을 찾을 수 없습니다."),
    ROUTE_STOP_NOT_FOUND(HttpStatus.NOT_FOUND, "ROUTE_002", "정류장을 찾을 수 없습니다."),

    STUDENT_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDENT_001", "학생을 찾을 수 없습니다."),

    REDIS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "REDIS_001", "Redis 처리 중 오류가 발생했습니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_001", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(
            HttpStatus status,
            String code,
            String message
    ) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}

