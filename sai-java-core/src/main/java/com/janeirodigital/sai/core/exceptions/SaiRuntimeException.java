package com.janeirodigital.sai.core.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Runtime (unchecked) exception raised performing SAI-specific operations
 */
@Getter @AllArgsConstructor
public class SaiRuntimeException extends RuntimeException {
    private final String message;
}
