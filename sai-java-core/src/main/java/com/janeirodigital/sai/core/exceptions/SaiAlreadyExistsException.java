package com.janeirodigital.sai.core.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Exception raised when a given requested type of data cannot be created because it
 * already exists
 */
@Getter @AllArgsConstructor
public class SaiAlreadyExistsException extends Exception {
    private final String message;
}
