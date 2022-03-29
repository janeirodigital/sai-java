package com.janeirodigital.sai.core.exceptions;

/**
 * Exception raised when a given requested type of data is missing or cannot be found
 */
public class SaiNotFoundException extends Exception {
    public SaiNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    public SaiNotFoundException(String message) {
        super(message);
    }
}
