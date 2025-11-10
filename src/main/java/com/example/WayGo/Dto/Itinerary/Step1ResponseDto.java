package com.example.WayGo.Dto.Itinerary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Step 1: 도시 검색 결과
 *
 * Google Places API 응답을 변환한 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Step1ResponseDto {

    /**
     * Google Place ID
     */
    private String placeId;

    /**
     * 도시명
     */
    private String cityName;

    /**
     * 전체 주소
     */
    private String formattedAddress;
}