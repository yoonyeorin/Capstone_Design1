package com.example.WayGo.Dto.Itinerary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * TEXT 탭 / 드롭다운 화면에서 사용할 전체 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryTextResponseDto {

    private Long itineraryId;
    private String title;
    private String destinationCity;

    private String startDate;
    private String endDate;

    private Integer totalDays;
    private Integer totalBudget;
    private Integer totalSpent;

    private List<ItineraryDayTextDto> days;   // 각 Day의 dayHeader + dayContent
}
