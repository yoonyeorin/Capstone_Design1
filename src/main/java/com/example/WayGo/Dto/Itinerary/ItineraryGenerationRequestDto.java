package com.example.WayGo.Dto.Itinerary;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 일정 생성 요청 DTO
 *
 * API: POST /api/itinerary/generate
 *
 * 요청 예시:
 * {
 *   "inputId": 1
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryGenerationRequestDto {

    /**
     * 입력 ID (8단계 완료된 입력)
     */
    @NotNull(message = "입력 ID는 필수입니다")
    private Integer inputId;
}