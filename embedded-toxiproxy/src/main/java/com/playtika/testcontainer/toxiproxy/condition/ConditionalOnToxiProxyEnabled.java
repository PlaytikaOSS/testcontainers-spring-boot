package com.playtika.testcontainer.toxiproxy.condition;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(ToxiProxyCondition.class)
public @interface ConditionalOnToxiProxyEnabled {

    /**
     * By default empty, means any module can enable this condition via `embedded.toxiproxy.proxies.{module}.enabled` property.
     * Specify specific module name if you want to enable it for specific module only.
     */
    String module() default "";
}
