package com.example.WayGo.Dto.Itinerary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 비행기표 추천 결과
 *
 * hasTicket = false일 때 반환
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightRecommendationDto {

    /**
     * 항공편 번호
     */
    private String flightNumber;

    /**
     * 항공사명
     */
    private String airline;

    /**
     * 출발 시간
     */
    private LocalDateTime departure;

    /**
     * 도착 시간
     */
    private LocalDateTime arrival;

    /**
     * 가격
     */
    private Integer price;

    /**
     * 통화
     */
    private String currency;
}