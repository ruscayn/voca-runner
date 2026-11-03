package org.ruscayn.vocarunner.common;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 영속성 어댑터(나가는 포트 …PersistencePort 를 DB 로 구현)라는 표시.
 * 책은 @Component 를 쓰지만, 시험 어댑터에 붙어 있던 @Repository 의 DB 예외 변환을 그대로 두려고 @Repository 로 한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repository
public @interface PersistenceAdapter {

    @AliasFor(annotation = Repository.class)
    String value() default "";
}
