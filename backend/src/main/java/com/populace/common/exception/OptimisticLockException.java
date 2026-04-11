package com.populace.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an optimistic lock conflict is detected.
 * This occurs when a record has been modified by another user since it was loaded.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class OptimisticLockException extends RuntimeException {

    private final String code;

    public OptimisticLockException(String message) {
        super(message);
        this.code = "VERSION_MISMATCH";
    }

    public OptimisticLockException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
