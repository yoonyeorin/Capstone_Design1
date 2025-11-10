package com.example.WayGo.Validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 일정 밀도 유효성 검사 어노테이션
 *
 * 검증 규칙:
 * - "RELAXED" 또는 "PACKED"만 허용
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ScheduleDensityValidator.class)
@Documented
public @interface ValidScheduleDensity {

    String message() default "일정 밀도는 'RELAXED' 또는 'PACKED'만 선택 가능합니다";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}