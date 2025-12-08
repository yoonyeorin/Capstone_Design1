package com.example.WayGo.Dto.Itinerary;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GooglePlaceDto {

    private String name;        // 장소명
    private String address;     // 주소
    private String placeId;     // Google Place ID
    private double rating;      // 평점
    private double lat;         // 위도
    private double lng;         // 경도
}
