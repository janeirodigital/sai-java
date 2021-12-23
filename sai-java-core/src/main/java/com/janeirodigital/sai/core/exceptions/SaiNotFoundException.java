package com.janeirodigital.sai.core.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class SaiNotFoundException extends Exception {
    private final String message;
}
