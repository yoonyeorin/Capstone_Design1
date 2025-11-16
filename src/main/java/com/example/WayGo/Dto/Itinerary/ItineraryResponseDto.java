package com.example.WayGo.Dto.Itinerary;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryResponseDto {

    private Long itineraryId;        // 일정 ID

    private String title;            // 제목 ("도쿄 2박3일")
    private String destinationCity;  // 여행 도시 ("도쿄")

    private String startDate;        // 시작일
    private String endDate;          // 종료일
    private Integer totalDays;       // 총 여행일수

    private Integer totalBudget;     // 전체 예산
    private Integer totalSpent;      // 총 지출

    private List<ItineraryDayDto> days; // 일자별 상세 일정
}
