package com.example.WayGo.Validation;

import com.example.WayGo.Entity.enums.ScheduleDensity;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScheduleDensityValidator
        implements ConstraintValidator<ValidScheduleDensity, String> {

    private static final Set<String> VALID_VALUES =
            Stream.of(ScheduleDensity.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        if (!VALID_VALUES.contains(value.toUpperCase())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("'%s'는(은) 유효하지 않은 일정 밀도입니다. 허용값: %s",
                            value, String.join(", ", VALID_VALUES))
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}