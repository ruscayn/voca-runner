package org.ruscayn.vocarunner.common;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

/** record 는 상속을 못 해 추상 클래스 대신 정적 메서드로 둔다 */
public final class SelfValidating {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private SelfValidating() {
    }

    public static <T> void validate(T target) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(target);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
