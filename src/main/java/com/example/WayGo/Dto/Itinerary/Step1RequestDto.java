package com.example.WayGo.Dto.Itinerary;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Step 1: 도시 검색 요청
 *
 * API: POST /api/itinerary/input/step1/search-city
 *
 * 요청 예시:
 * {
 *   "cityName": "도쿄"
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Step1RequestDto {

    @NotBlank(message = "도시명을 입력해주세요")
    private String cityName;
}