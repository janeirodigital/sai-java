package com.janeirodigital.sai.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to exclude select methods from code coverage processing.
 * It should be used SPARINGLY, and only in cases where it is impractical AND borderline
 * not possible to realize full code coverage.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExcludeFromGeneratedCoverage { }
