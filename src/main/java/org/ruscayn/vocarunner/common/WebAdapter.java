package org.ruscayn.vocarunner.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * 웹 어댑터(화면 · API 컨트롤러)라는 표시. 요청을 받으려면 @Controller · @RestController 도 함께 붙인다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface WebAdapter {

    @AliasFor(annotation = Component.class)
    String value() default "";
}
