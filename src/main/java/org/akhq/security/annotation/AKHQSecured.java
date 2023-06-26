package org.akhq.security.annotation;

import org.akhq.configs.security.Role;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface AKHQSecured {
    Role.Resource resource();
    Role.Action action();
}