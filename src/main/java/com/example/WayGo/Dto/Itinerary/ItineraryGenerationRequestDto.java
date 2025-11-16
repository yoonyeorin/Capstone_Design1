package com.example.WayGo.Dto.Itinerary;

import lombok.*;

import java.util.List;

/** 일정 생성 요청 DTO */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryGenerationRequestDto {

    private String destinationCity;      // 여행 도시 ("도쿄")
    private String startDate;            // 여행 시작일 ("2025-11-09")
    private String endDate;              // 여행 종료일 ("2025-11-11")

    private String arrivalTime;          // 첫날 도착 시간 ("09:00")
    private String scheduleDensity;      // RELAXED / PACKED

    private List<String> travelStyles;   // 여행 스타일 ["FOOD", "CULTURE"]

    private Integer budget;              // 전체 예산
    private Boolean needsHotel;          // 숙소 추천 여부
}
