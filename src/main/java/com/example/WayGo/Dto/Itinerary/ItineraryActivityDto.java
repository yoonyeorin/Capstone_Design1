package com.example.WayGo.Dto.Itinerary;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 활동 상세 DTO
 *
 * 09:30  🏯 아사쿠사 센소지
 *        ├─ 체류: 1시간 30분
 *        └─ 입장료: 무료
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryActivityDto {

    /**
     * 활동 ID
     */
    private Long activityId;

    /**
     * 순서
     */
    private Integer sequence;

    /**
     * 활동 타입 (PLACE, MEAL, ACCOMMODATION)
     */
    private String activityType;

    /**
     * 장소명
     */
    private String placeName;

    /**
     * Google Place ID
     */
    private String placeId;

    /**
     * 주소
     */
    private String address;

    /**
     * 시작 시간
     */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    /**
     * 종료 시간
     */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    /**
     * 체류 시간 (분)
     */
    private Integer durationMinutes;

    /**
     * 입장료
     */
    private Integer entranceFee;

    /**
     * 식비
     */
    private Integer mealCost;

    /**
     * 평점
     */
    private BigDecimal rating;

    /**
     * 다음 장소로 가는 교통수단
     */
    private String transportToNext;

    /**
     * 이동 시간 (분)
     */
    private Integer transportDuration;

    /**
     * 이동 비용
     */
    private Integer transportCost;

    /**
     * 꿀팁
     */
    private String tips;
}