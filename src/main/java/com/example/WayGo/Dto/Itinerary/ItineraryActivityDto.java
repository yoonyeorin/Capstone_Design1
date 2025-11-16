package com.example.WayGo.Dto.Itinerary;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryActivityDto {

    private Integer sequence;           // 하루 순서 (1~N)

    private String activityType;        // PLACE / MEAL / MOVE / HOTEL 등

    private String name;                // 장소/식당/숙소 이름
    private String address;             // 주소

    private String startTime;           // 시작 시간 ("09:30")
    private String endTime;             // 종료 시간 ("11:00")
    private Integer durationMinutes;    // 활동 시간(분)

    private String transportType;       // 이동 수단 (walk / bus / subway / taxi)
    private Integer transportDuration;  // 이동 시간(분)
    private Integer transportCost;      // 이동 비용(원)

    private Integer entranceFee;        // 입장료
    private Integer mealCost;           // 식비

    private Double rating;              // 구글 평점
    private String tip;                 // 관람 팁 / 여유시간 팁
}
