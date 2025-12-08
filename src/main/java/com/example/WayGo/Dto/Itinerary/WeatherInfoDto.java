package com.example.WayGo.Dto.Itinerary;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 날짜별 날씨 정보 DTO
 * - 일자 하나에 대한 날씨 데이터를 담는다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherInfoDto {

    private String date;          // "2025-11-09"
    private String condition;     // 날씨 요약 ("Clear", "Rain")
    private String conditionKr;   // 한국어 날씨 ("맑음", "비")
    private Integer temperature;  // 평균 기온
    private Integer tempMin;      // 최저 기온
    private Integer tempMax;      // 최고 기온
    private Boolean isRainy;      // 비 여부
    private String advice;        // 옷차림 / 휴대물품 조언
}
