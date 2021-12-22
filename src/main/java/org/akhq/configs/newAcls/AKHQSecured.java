package org.akhq.configs.newAcls;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface AKHQSecured {
    Permission.Resource resource();
    Permission.Role role();
}
