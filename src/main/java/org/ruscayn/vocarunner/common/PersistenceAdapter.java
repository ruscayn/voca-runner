package org.ruscayn.vocarunner.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Repository;

/** 책은 @Component 를 쓰지만 DB 예외 변환을 그대로 두려고 @Repository 를 바탕으로 한다 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repository
public @interface PersistenceAdapter {

    @AliasFor(annotation = Repository.class)
    String value() default "";

}
