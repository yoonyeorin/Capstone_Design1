package com.example.WayGo.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 실시간 인기 도시 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingCityDTO {

    private Integer rank;           // 순위 (1~10)
    private String cityCode;        // IATA 도시 코드 (예: PAR, TYO)
    private String cityName;        // 도시명 (예: Paris, Tokyo)
    private String countryName;     // 국가명 (예: France, Japan)
    private Integer travelScore;    // 여행 점수 (Amadeus 데이터 기반)
}

