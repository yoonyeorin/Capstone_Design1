package com.example.WayGo.Dto.Itinerary;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Step 7: 예산
 *
 * API: PUT /api/itinerary/input/{inputId}/step7
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Step7RequestDto {

    @NotNull(message = "예산을 입력해주세요")
    @Min(value = 10000, message = "최소 10,000원 이상이어야 합니다")
    private Integer budget;
}