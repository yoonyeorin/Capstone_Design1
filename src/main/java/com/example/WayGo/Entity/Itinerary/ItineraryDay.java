package com.example.WayGo.Entity.Itinerary;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "itinerary_days")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어느 일정에 속한 날짜인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    // 1일차, 2일차 …
    @Column(nullable = false)
    private Integer dayNumber;

    @Column(nullable = false)
    private LocalDate date;

    // 날씨 요약 (맑음, 비, 흐림 등)
    private String weatherCondition;

    // 기온 (대략 평균)
    private Integer temperature;

    // "해가 쨍쨍합니다, 모자를 챙겨주세요" 이런 텍스트
    @Column(length = 500)
    private String weatherAdvice;

    // 하루 예산 / 실제 지출
    private Integer dailyBudget;
    private Integer dailySpent;
}
