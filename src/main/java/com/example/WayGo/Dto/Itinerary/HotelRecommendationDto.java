package com.example.WayGo.Dto.Itinerary;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelRecommendationDto {
    private String name;
    private String address;
    private Double rating;
    private Integer price;        // 평균 1박 가격
    private String currency;      // KRW
    private Double lat;
    private Double lng;

    // ✅ OpenAIService에서 쓰기 편하게 만든 래퍼 메서드
    public Integer getPricePerNight() {
        return price;
    }
}
