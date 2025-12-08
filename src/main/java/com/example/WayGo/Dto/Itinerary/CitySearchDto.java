package com.example.WayGo.Dto.Itinerary;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitySearchDto {

    private String name;       // 도시명
    private String fullName;   // 전체 주소
    private Double lat;        // 위도
    private Double lng;        // 경도
    private String placeId;    // Google Place ID
}
