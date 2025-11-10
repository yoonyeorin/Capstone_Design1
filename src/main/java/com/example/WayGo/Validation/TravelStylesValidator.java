package com.example.WayGo.Validation;

import com.example.WayGo.Entity.enums.TravelStyle;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TravelStylesValidator
        implements ConstraintValidator<ValidTravelStyles, List<String>> {

    private static final Set<String> VALID_VALUES =
            Stream.of(TravelStyle.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());

    @Override
    public boolean isValid(List<String> values, ConstraintValidatorContext context) {
        if (values == null) {
            return false;
        }

        if (values.isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "여행 취향을 최소 1개 선택해주세요"
            ).addConstraintViolation();
            return false;
        }

        if (values.size() > 2) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "여행 취향은 최대 2개까지만 선택 가능합니다"
            ).addConstraintViolation();
            return false;
        }

        for (String value : values) {
            if (value == null || value.trim().isEmpty()) {
                return false;
            }

            if (!VALID_VALUES.contains(value.toUpperCase())) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        String.format("'%s'는(은) 유효하지 않은 여행 취향입니다. 허용값: %s",
                                value, String.join(", ", VALID_VALUES))
                ).addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}