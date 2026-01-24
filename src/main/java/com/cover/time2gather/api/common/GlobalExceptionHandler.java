package com.cover.time2gather.api.common;

import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import com.cover.time2gather.util.MessageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 전역 예외 처리기
 * Accept-Language 헤더 기반으로 다국어 에러 메시지를 반환합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());

        String message;
        if (e.hasErrorCode()) {
            message = MessageProvider.getMessage(e.getErrorCode().getMessageKey(), e.getArgs());
        } else {
            message = e.getMessage();
        }

        return ResponseEntity
                .status(e.getHttpStatus())
                .body(ApiResponse.error(message));
    }

    /**
     * Bean Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining(", "));

        log.warn("Validation exception: {}", errorMessage);

        String fallbackMessage = MessageProvider.getMessage(ErrorCode.INVALID_INPUT.getMessageKey());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorMessage.isEmpty() ? fallbackMessage : errorMessage));
    }

    /**
     * 인증 예외 처리
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException e) {
        log.warn("Authentication exception: {}", e.getMessage());
        String message = MessageProvider.getMessage(ErrorCode.AUTH_REQUIRED_LOGIN.getMessageKey());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(message));
    }

    /**
     * 접근 권한 예외 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied exception: {}", e.getMessage());
        String message = MessageProvider.getMessage(ErrorCode.ACCESS_DENIED.getMessageKey());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(message));
    }

    /**
     * IllegalArgumentException 처리 (잘못된 요청 파라미터)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument exception: {}", e.getMessage());
        String detailMessage = e.getMessage() != null ? e.getMessage() : "";
        String message = MessageProvider.getMessage(ErrorCode.INVALID_REQUEST.getMessageKey(), detailMessage);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    /**
     * IllegalStateException 처리 (잘못된 상태에서의 요청)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException e) {
        log.warn("Illegal state exception: {}", e.getMessage());
        String message = e.getMessage() != null 
                ? e.getMessage() 
                : MessageProvider.getMessage(ErrorCode.INVALID_STATE.getMessageKey());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    /**
     * NullPointerException 처리
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Void>> handleNullPointerException(NullPointerException e) {
        log.error("Null pointer exception", e);
        String message = MessageProvider.getMessage(ErrorCode.NULL_POINTER.getMessageKey());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }

    /**
     * NumberFormatException 처리
     */
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ApiResponse<Void>> handleNumberFormatException(NumberFormatException e) {
        log.warn("Number format exception: {}", e.getMessage());
        String message = MessageProvider.getMessage(ErrorCode.NUMBER_FORMAT.getMessageKey());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    /**
     * NoResourceFoundException 처리 (정적 리소스 요청)
     * favicon.ico, 존재하지 않는 정적 파일 요청 등을 조용히 처리
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.debug("Static resource not found: {}", e.getResourcePath());
        String message = MessageProvider.getMessage(ErrorCode.RESOURCE_NOT_FOUND.getMessageKey());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    /**
     * 기타 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected exception", e);
        String message = MessageProvider.getMessage(ErrorCode.INTERNAL_SERVER_ERROR.getMessageKey());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }

    private String formatFieldError(FieldError fieldError) {
        return String.format("%s: %s", fieldError.getField(), fieldError.getDefaultMessage());
    }
}
