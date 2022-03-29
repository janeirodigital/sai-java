package com.janeirodigital.sai.core.exceptions;

/**
 * Runtime (unchecked) exception raised performing SAI-specific operations
 */
public class SaiRuntimeException extends RuntimeException {
    public SaiRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
