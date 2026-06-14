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
    WEAK_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호는 10자 이상이어야 하며 흔한 비밀번호나 이메일, 닉네임을 포함할 수 없습니다."),

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "인증번호가 올바르지 않거나 만료되었습니다."),
    INVALID_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST, "인증 정보가 올바르지 않거나 만료되었습니다."),
    VERIFICATION_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해 주세요."),
    VERIFICATION_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "인증번호는 1분 후 다시 요청할 수 있습니다."),

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
    IMPOSSIBLE_SPEED(HttpStatus.CONFLICT, "기록된 속도가 물리적으로 불가능한 값입니다."),
    ALREADY_REPORTED(HttpStatus.CONFLICT, "이미 신고한 코스입니다."),
    INVALID_REPORT_REASON(HttpStatus.BAD_REQUEST, "유효하지 않은 신고 사유입니다."),
    COURSE_PUBLISH_METADATA_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "공개에 필요한 메타데이터를 모두 입력해주세요."),
    COURSE_PUBLISH_NOT_ENOUGH_COMPLETIONS(HttpStatus.UNPROCESSABLE_ENTITY, "공개하려면 이 코스를 10회 이상 완주해야 합니다."),

    // 503 Service Unavailable
    TRAINING_RECOMMENDATION_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "AI 훈련 추천을 생성하지 못했습니다. 잠시 후 다시 시도해 주세요."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
