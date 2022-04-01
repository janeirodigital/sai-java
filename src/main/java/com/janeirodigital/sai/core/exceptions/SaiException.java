package com.janeirodigital.sai.core.exceptions;

/**
 * General exception used to represent issues in sai-java processing logic
 */
public class SaiException extends Exception {
    public SaiException(String message, Throwable cause) {
        super(message, cause);
    }
    public SaiException(String message) {
        super(message);
    }
}
