package com.example.WayGo.Dto.Itinerary;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceScoreDto {

    private PlaceDto place;   // 원본 장소 정보
    private double score;     // 계산된 점수 (높을수록 좋음)
    private int distanceMeters;  // 기준 위치까지 거리
    private int durationMinutes; // 이동 시간 (분)
}
