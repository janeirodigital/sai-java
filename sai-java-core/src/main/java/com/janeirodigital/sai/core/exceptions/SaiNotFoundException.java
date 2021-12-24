package com.janeirodigital.sai.core.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Exception raised when a given requested type of data is missing or cannot be found
 */
@Getter @AllArgsConstructor
public class SaiNotFoundException extends Exception {
    private final String message;
}
