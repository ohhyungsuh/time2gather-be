package com.cover.time2gather.domain.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 로직 예외의 기본 클래스
 * ErrorCode 기반으로 다국어 메시지를 지원합니다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final ErrorCode errorCode;
    private final Object[] args;

    // ===== ErrorCode 기반 생성자 (권장) =====

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getHttpStatus();
        this.args = null;
    }

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getHttpStatus();
        this.args = args;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessageKey(), cause);
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getHttpStatus();
        this.args = null;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode.getMessageKey(), cause);
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getHttpStatus();
        this.args = args;
    }

    // ===== 기존 String 메시지 기반 생성자 (하위 호환성) =====

    public BusinessException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = null;
        this.args = null;
    }

    public BusinessException(String message, Throwable cause) {
        this(message, HttpStatus.BAD_REQUEST, cause);
    }

    public BusinessException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = null;
        this.args = null;
    }

    /**
     * ErrorCode가 있는지 확인
     */
    public boolean hasErrorCode() {
        return errorCode != null;
    }
}
