package com.example.WayGo.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 일별 일정 Entity
 *
 * 각 날짜(1일차, 2일차...)의 정보를 저장
 * - 날짜, 날씨 정보
 * - 하루 예산
 * - 활동 목록
 */
@Entity
@Table(name = "itinerary_days")
@Getter
@Setter
public class ItineraryDay {

    // ============================================================
    // 기본 PK
    // ============================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================================
    // 연관관계
    // ============================================================

    /**
     * 어느 여행의 일정인지
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    /**
     * 활동 목록 (관광지, 식사, 숙소)
     */
    @OneToMany(mappedBy = "itineraryDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<ItineraryActivity> activities = new ArrayList<>();

    // ============================================================
    // 날짜 정보
    // ============================================================

    /**
     * 1일차, 2일차, 3일차...
     */
    @Column(nullable = false)
    private Integer dayNumber;

    /**
     * 실제 날짜 (2024-11-09)
     */
    @Column(nullable = false)
    private LocalDate date;

    // ============================================================
    // 날씨 정보
    // ============================================================

    /**
     * 날씨 상태 (맑음, 비, 흐림)
     */
    @Column(length = 50)
    private String weatherCondition;

    /**
     * 기온 (도)
     */
    @Column
    private Integer temperature;

    /**
     * 날씨 기반 조언
     * 예: "우산 챙기세요", "모자를 챙겨주세요"
     */
    @Column(columnDefinition = "TEXT")
    private String weatherAdvice;

    // ============================================================
    // 예산 관리
    // ============================================================

    /**
     * 하루 예산 (총예산 ÷ 일수)
     */
    @Column
    private Integer dailyBudget;

    /**
     * 하루 예상 지출
     */
    @Column(nullable = false)
    private Integer dailySpent = 0;

    // ============================================================
    // 연관관계 편의 메서드
    // ============================================================

    /**
     * 활동 추가
     */
    public void addActivity(ItineraryActivity activity) {
        this.activities.add(activity);
        activity.setItineraryDay(this);
    }

    /**
     * 활동 제거
     */
    public void removeActivity(ItineraryActivity activity) {
        this.activities.remove(activity);
        activity.setItineraryDay(null);
    }
}