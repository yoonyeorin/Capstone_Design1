package com.example.WayGo.Dto.Itinerary;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItinerarySummaryDto {

    private Long itineraryId;         // 일정 ID
    private String date;              // 날짜 ("2025-11-09")
    private List<String> mainPlaces;  // 대표 장소 목록 (2~4개)
    private String weather;           // 날씨 (sunny / rain / cloudy)
    private Integer temperature;      // 평균 기온
    private String scheduleDensity;   // 일정 밀도 (RELAXED / PACKED)
}
