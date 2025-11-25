package com.example.WayGo.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlacesSearchRequest {
    private Double latitude;    // 현재 위치 위도
    private Double longitude;   // 현재 위치 경도
    private String category;    // 카테고리 (restroom, hospital, pharmacy, gas_station)
    private Integer radius = 1000;  // 반경 (기본 1km = 1000m)
}