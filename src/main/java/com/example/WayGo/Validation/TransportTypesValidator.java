package com.example.WayGo.Validation;

import com.example.WayGo.Entity.enums.TransportType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TransportType 검증 로직
 *
 * ConstraintValidator<A, T>:
 * - A: 어노테이션 타입 (ValidTransportTypes)
 * - T: 검증할 데이터 타입 (List<String>)
 *
 * 동작 과정:
 * 1. Enum의 모든 값을 Set으로 변환 (BUS, SUBWAY, TAXI, WALK, CAR)
 * 2. 입력받은 List<String>의 각 요소가 Set에 있는지 확인
 * 3. 하나라도 없으면 false (검증 실패)
 */
public class TransportTypesValidator
        implements ConstraintValidator<ValidTransportTypes, List<String>> {

    /**
     * 허용되는 교통수단 목록
     *
     * Stream.of(TransportType.values())
     * → [BUS, SUBWAY, TAXI, WALK, CAR]
     *
     * .map(Enum::name)
     * → ["BUS", "SUBWAY", "TAXI", "WALK", "CAR"]
     *
     * .collect(Collectors.toSet())
     * → Set{"BUS", "SUBWAY", "TAXI", "WALK", "CAR"}
     *
     * Set을 쓰는 이유:
     * - contains() 연산이 O(1)로 빠름 (List는 O(n))
     * - 중복 자동 제거
     */
    private static final Set<String> VALID_VALUES =
            Stream.of(TransportType.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());

    /**
     * 초기화 메서드
     * 어노테이션에서 설정한 값을 읽어올 수 있음
     * (여기서는 사용 안 함)
     */
    @Override
    public void initialize(ValidTransportTypes constraintAnnotation) {
        // 필요하면 어노테이션 속성 읽어서 초기화
    }

    /**
     * 실제 검증 로직
     *
     * @param values: 검증할 값 (["BUS", "subway", "walk"])
     * @param context: 검증 컨텍스트 (에러 메시지 커스터마이징 가능)
     * @return true면 검증 성공, false면 실패
     */
    @Override
    public boolean isValid(List<String> values, ConstraintValidatorContext context) {
        // null 체크
        if (values == null) {
            return false;  // null은 허용 안 함
        }

        // 빈 리스트 체크
        if (values.isEmpty()) {
            // 커스텀 에러 메시지 설정
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "교통수단을 최소 1개 이상 선택해주세요"
            ).addConstraintViolation();
            return false;
        }

        // 각 요소 검증
        for (String value : values) {
            // null이나 빈 문자열 체크
            if (value == null || value.trim().isEmpty()) {
                return false;
            }

            // 대소문자 무시하고 유효한 값인지 확인
            // "bus" → "BUS" 변환 후 체크
            if (!VALID_VALUES.contains(value.toUpperCase())) {
                // 어떤 값이 잘못됐는지 알려주는 친절한 에러 메시지
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        String.format("'%s'는(은) 유효하지 않은 교통수단입니다. 허용값: %s",
                                value,
                                String.join(", ", VALID_VALUES))
                ).addConstraintViolation();
                return false;
            }
        }

        return true;  // 모든 검증 통과!
    }
}