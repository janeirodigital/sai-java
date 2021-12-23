package com.janeirodigital.sai.core.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class SaiException extends Exception {
    private final String message;
}
