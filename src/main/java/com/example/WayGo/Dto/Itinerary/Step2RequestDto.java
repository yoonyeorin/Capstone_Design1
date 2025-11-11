package com.example.WayGo.Dto.Itinerary;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Step 2: 날짜 선택 요청
 *
 * API: PUT /api/itinerary/input/{inputId}/step2
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Step2RequestDto {

    @NotNull(message = "가는 날짜를 선택해주세요")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "돌아오는 날짜를 선택해주세요")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @AssertTrue(message = "여행 기간은 1일 이상 14일 이하여야 합니다")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return false;
        }

        if (endDate.isBefore(startDate)) {
            return false;
        }

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return days >= 1 && days <= 14;
    }
}