package com.runway.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값 검증에 실패했습니다."),

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    RUN_NOT_FOUND(HttpStatus.NOT_FOUND, "러닝 기록을 찾을 수 없습니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다."),
    COURSE_ATTEMPT_NOT_FOUND(HttpStatus.NOT_FOUND, "코스 시도를 찾을 수 없습니다."),

    // 409 Conflict
    DUPLICATED_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATED_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    INVALID_RUN_STATUS(HttpStatus.CONFLICT, "현재 러닝 상태에서는 해당 동작을 수행할 수 없습니다."),
    INVALID_COURSE_STATUS(HttpStatus.CONFLICT, "현재 코스 상태에서는 해당 동작을 수행할 수 없습니다."),
    INVALID_ATTEMPT_STATUS(HttpStatus.CONFLICT, "현재 코스 시도 상태에서는 해당 동작을 수행할 수 없습니다."),
    NOT_COMPLETED_RUN(HttpStatus.CONFLICT, "완료된 러닝 기록만 코스로 생성할 수 있습니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
