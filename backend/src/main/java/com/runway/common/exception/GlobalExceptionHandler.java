package com.runway.common.exception;

import com.runway.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RunwayException.class)
    public ResponseEntity<ApiResponse<?>> handleRunwayException(RunwayException e) {
        ErrorCode code = e.getErrorCode();
        log.warn("RunwayException [{}]: {}", code.name(), e.getMessage());
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.error(e.getMessage(), code.name()));
    }

    // @Valid, @Validated 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse(ErrorCode.VALIDATION_ERROR.getMessage());
        log.warn("ValidationException: {}", message);
        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(ApiResponse.error(message, ErrorCode.VALIDATION_ERROR.name()));
    }

    // JSON 파싱 실패 (잘못된 형식의 요청 바디)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST.getMessage(), ErrorCode.INVALID_REQUEST.name()));
    }

    // 필수 query parameter 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingParam(MissingServletRequestParameterException e) {
        String message = "필수 파라미터가 누락되었습니다: " + e.getParameterName();
        log.warn("MissingServletRequestParameterException: {}", message);
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(ApiResponse.error(message, ErrorCode.INVALID_REQUEST.name()));
    }

    // 그 외 예상치 못한 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Unexpected exception: {}", e.getMessage(), e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.error(
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                        ErrorCode.INTERNAL_SERVER_ERROR.name()
                ));
    }
}
