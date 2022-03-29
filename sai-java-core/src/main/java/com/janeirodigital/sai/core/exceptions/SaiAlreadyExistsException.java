package com.janeirodigital.sai.core.exceptions;

/**
 * Exception raised when a given requested type of data cannot be created because it
 * already exists
 */
public class SaiAlreadyExistsException extends Exception {
    public SaiAlreadyExistsException(String message) {
        super(message);
    }
}
