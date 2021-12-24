package com.janeirodigital.sai.core.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * General exception used to represent issues in sai-java processing logic
 */
@Getter @AllArgsConstructor
public class SaiException extends Exception {
    private final String message;
}
