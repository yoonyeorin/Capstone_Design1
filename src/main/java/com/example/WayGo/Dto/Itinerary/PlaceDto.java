package com.example.WayGo.Dto.Itinerary;

import lombok.*;
import java.util.List;

/**
 * 장소 정보 DTO
 * - Google Places API 결과에서 필요한 데이터만 추출함
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceDto {

    private String name;        // 장소 이름
    private String address;     // 주소
    private Double lat;         // 위도
    private Double lng;         // 경도
    private Double rating;      // 평점 (4.5 등)
    private String placeId;     // 구글 장소 ID


    private List<String> styleTags;
}
