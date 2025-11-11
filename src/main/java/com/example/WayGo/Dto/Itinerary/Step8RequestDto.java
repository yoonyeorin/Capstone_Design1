package com.example.WayGo.Dto.Itinerary;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Step 8: 숙소 추천
 *
 * API: PUT /api/itinerary/input/{inputId}/step8
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Step8RequestDto {

    @NotNull(message = "숙소 추천 여부를 선택해주세요")
    private Boolean needsAccommodation;

    @Min(value = 10000, message = "숙소 예산은 최소 10,000원 이상이어야 합니다")
    private Integer accommodationBudget;

    @AssertTrue(message = "숙소 추천을 원하시면 예산을 입력해주세요")
    public boolean isValidAccommodationBudget() {
        if (Boolean.FALSE.equals(needsAccommodation)) {
            return true;
        }

        return accommodationBudget != null && accommodationBudget >= 10000;
    }
}