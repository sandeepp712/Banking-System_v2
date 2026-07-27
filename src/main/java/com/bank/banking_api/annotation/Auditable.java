package com.bank.banking_api.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {
    String action() default "UNKNOWN_ACTION";
}