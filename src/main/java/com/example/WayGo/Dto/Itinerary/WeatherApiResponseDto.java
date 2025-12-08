package com.example.WayGo.Dto.Itinerary;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * OpenWeatherMap API 응답 구조 DTO
 * - 외부 API 원본 데이터를 매핑하는 용도
 * - 서비스 내부 전용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WeatherApiResponseDto {

    private List<Daily> daily;  // 7~16일 예보

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Daily {
        private Long dt;       // 날짜 (timestamp)
        private Temp temp;     // 기온 정보
        private List<WeatherDesc> weather; // 날씨 설명
        private Double pop;    // 강수 확률 (0.0 ~ 1.0)
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Temp {
        private Double day;    // 평균 기온
        private Double min;    // 최저
        private Double max;    // 최고
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherDesc {
        private String main;        // "Rain", "Clear"
        private String description; // "light rain"
    }
}
