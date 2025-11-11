package com.example.WayGo.Dto.Itinerary;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Step 4: 인원수
 *
 * API: PUT /api/itinerary/input/{inputId}/step4
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Step4RequestDto {

    @NotNull(message = "인원수를 입력해주세요")
    @Min(value = 1, message = "최소 1명 이상이어야 합니다")
    @Max(value = 10, message = "최대 10명까지 가능합니다")
    private Integer numberOfPeople;
}