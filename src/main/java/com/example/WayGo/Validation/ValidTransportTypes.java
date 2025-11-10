package com.example.WayGo.Validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 교통수단 유효성 검사 어노테이션
 *
 * 사용법:
 * @ValidTransportTypes
 * private List<String> transportTypes;
 *
 * 검증 규칙:
 * 1. null이나 빈 리스트 불가
 * 2. "BUS", "SUBWAY", "TAXI", "WALK", "CAR"만 허용
 * 3. 대소문자 구분 안 함 ("bus" → "BUS"로 자동 변환 후 검증)
 *
 * 에러 메시지:
 * "유효하지 않은 교통수단입니다. (BUS, SUBWAY, TAXI, WALK, CAR)"
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})  // 필드나 파라미터에 붙일 수 있음
@Retention(RetentionPolicy.RUNTIME)  // 런타임에 동작
@Constraint(validatedBy = TransportTypesValidator.class)  // 실제 검증 로직
@Documented
public @interface ValidTransportTypes {

    /**
     * 에러 메시지 (기본값)
     * 나중에 messages.properties에서 관리 가능
     */
    String message() default "유효하지 않은 교통수단입니다. 허용값: BUS, SUBWAY, TAXI, WALK, CAR";

    /**
     * 검증 그룹 (고급 기능, 일단 무시해도 됨)
     */
    Class<?>[] groups() default {};

    /**
     * 페이로드 (고급 기능, 일단 무시해도 됨)
     */
    Class<? extends Payload>[] payload() default {};
}