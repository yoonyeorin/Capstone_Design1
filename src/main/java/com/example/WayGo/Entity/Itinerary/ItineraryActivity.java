package com.example.WayGo.Entity.Itinerary;

import com.example.WayGo.Entity.enums.ActivityType;
import com.example.WayGo.Entity.enums.TransportType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "itinerary_activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어느 날에 속한 활동인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_id", nullable = false)
    private ItineraryDay itineraryDay;

    // 하루 안에서 순서 (1,2,3,…)
    @Column(nullable = false)
    private Integer sequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityType activityType;

    // 장소/식당/숙소 이름 등
    @Column(length = 200)
    private String name;

    // Google Places 같은 placeId (있으면 사용)
    @Column(length = 200)
    private String placeId;

    @Column(length = 300)
    private String address;

    private LocalTime startTime;
    private LocalTime endTime;

    // 분 단위 체류 시간
    private Integer durationMinutes;

    // 입장료 / 식사비
    private Integer entranceFee;
    private Integer mealCost;

    // 평점(4.5 같은 값) → 나중에 DTO에서 BigDecimal로 변환해도 됨
    private Double rating;

    // 이동 수단 정보
    @Enumerated(EnumType.STRING)
    private TransportType transportType;

    private Integer transportDuration; // 분
    private Integer transportCost;     // 원 단위

    // 활동별 TIP (여유시간, 추천 포인트 등)
    @Column(length = 1000)
    private String tip;
}
