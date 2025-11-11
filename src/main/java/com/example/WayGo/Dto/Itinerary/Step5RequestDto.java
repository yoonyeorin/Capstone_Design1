package com.example.WayGo.Dto.Itinerary;

import com.example.WayGo.Validation.ValidTransportTypes;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Step 5: 이동수단 선택
 *
 * API: PUT /api/itinerary/input/{inputId}/step5
 *
 * ⭐ String으로 받고, Service에서 Enum으로 변환!
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Step5RequestDto {

    @NotEmpty(message = "교통수단을 최소 1개 선택해주세요")
    @ValidTransportTypes
    private List<String> transportTypes;
}