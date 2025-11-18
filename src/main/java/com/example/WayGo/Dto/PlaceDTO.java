package com.example.WayGo.Dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceDTO {
    private String placeId;        // Google Place ID
    private String name;           // 장소 이름
    private Double latitude;       // 위도
    private Double longitude;      // 경도
    private String address;        // 주소
    private Double distance;       // 현재 위치로부터의 거리 (미터)
    private String category;       // 카테고리
    private Double rating;         // 평점 (선택사항)
    private Boolean openNow;       // 현재 영업 중 여부 (선택사항)
}