package com.example.WayGo.Dto.Itinerary;

import com.example.WayGo.Validation.ValidScheduleDensity;
import com.example.WayGo.Validation.ValidTravelStyles;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Step 6: 여행 취향 + 일정 밀도
 *
 * API: PUT /api/itinerary/input/{inputId}/step6
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Step6RequestDto {

    @NotEmpty(message = "여행 취향을 최소 1개 선택해주세요")
    @ValidTravelStyles
    private List<String> travelStyles;

    @NotBlank(message = "일정 밀도를 선택해주세요")
    @ValidScheduleDensity
    private String scheduleDensity;
}