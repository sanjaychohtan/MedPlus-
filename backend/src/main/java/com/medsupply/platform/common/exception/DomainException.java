package com.medsupply.platform.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base custom business domain exception.
 * Integrates an errorCode string and an explicit HTTP Status to route API failures accurately.
 */
@Getter
public class DomainException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public DomainException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public DomainException(String errorCode, String message) {
        this(errorCode, message, HttpStatus.BAD_REQUEST);
    }
}
