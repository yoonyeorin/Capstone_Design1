package com.example.WayGo.Validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 여행 취향 유효성 검사 어노테이션
 *
 * 검증 규칙:
 * 1. 최소 1개, 최대 2개 선택
 * 2. "ACTIVE", "RELAXED", "NATURE", "CULTURE", "FOOD", "CITY"만 허용
 * 3. 대소문자 구분 안 함
 * 4. 중복 선택 불가 (["FOOD", "FOOD"] → 에러)
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TravelStylesValidator.class)
@Documented
public @interface ValidTravelStyles {

    String message() default "여행 취향은 1~2개를 선택해야 합니다. 허용값: ACTIVE, RELAXED, NATURE, CULTURE, FOOD, CITY";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}