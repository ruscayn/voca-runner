package org.ruscayn.vocarunner.common;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Set;

/**
 * 입력 객체(Command)가 만들어질 때 스스로 검증하게 하는 도우미 (책 『만들면서 배우는 클린 아키텍처』의 SelfValidating).
 * record 는 상속을 못 해 추상 클래스 대신 정적 메서드로 둔다. 어기면 ConstraintViolationException 을 던진다.
 */
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
