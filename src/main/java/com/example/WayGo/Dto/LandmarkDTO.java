package com.example.WayGo.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 랜드마크 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandmarkDTO {

    private String landmarkName;    // 랜드마크명 (예: 에펠탑)
    private String description;     // 설명
    private String imageUrl;        // 이미지 URL
    private Double rating;          // 평점 (0.0 ~ 5.0)
    private String address;         // 주소
}