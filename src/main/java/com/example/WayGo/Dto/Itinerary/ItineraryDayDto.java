package com.example.WayGo.Dto.Itinerary;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryDayDto {

    private Integer dayNumber;          // 몇 일차 (1, 2, 3...)
    private String date;                // 날짜 ("2025-11-09")

    private String weatherCondition;    // 날씨 ("맑음", "비")
    private Integer temperature;        // 기온
    private String weatherAdvice;       // 날씨 팁

    private Integer dailyBudget;        // 하루 예산
    private Integer dailySpent;         // 하루 지출

    private List<ItineraryActivityDto> activities;  // 활동 전체 리스트
}
