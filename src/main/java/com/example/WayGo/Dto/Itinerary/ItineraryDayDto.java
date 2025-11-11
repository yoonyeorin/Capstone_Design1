package com.example.WayGo.Dto.Itinerary;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 일별 일정 DTO
 *
 * 📅 1일차 - 11월 9일 (토) ☀️ 맑음 22°C
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryDayDto {

    /**
     * 일별 일정 ID
     */
    private Long dayId;

    /**
     * 몇일차 (1, 2, 3...)
     */
    private Integer dayNumber;

    /**
     * 날짜
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /**
     * 날씨 상태 (맑음, 비, 흐림)
     */
    private String weatherCondition;

    /**
     * 기온
     */
    private Integer temperature;

    /**
     * 날씨 조언
     * 예: "우산 챙기세요"
     */
    private String weatherAdvice;

    /**
     * 하루 예산
     */
    private Integer dailyBudget;

    /**
     * 하루 예상 지출
     */
    private Integer dailySpent;

    /**
     * 활동 목록 (시간순)
     */
    private List<ItineraryActivityDto> activities;
}