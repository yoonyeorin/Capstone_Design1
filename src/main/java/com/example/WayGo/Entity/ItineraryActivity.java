package com.example.WayGo.Entity;

import com.example.WayGo.Entity.enums.ActivityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 활동 상세 Entity
 *
 * 각 날짜의 시간대별 활동을 저장
 * - 관광지 방문
 * - 식사
 * - 숙소 체크인
 * - 이동 정보
 */
@Entity
@Table(name = "itinerary_activities")
@Getter
@Setter
public class ItineraryActivity {

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
     * 몇일차의 활동인지
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_day_id", nullable = false)
    private ItineraryDay itineraryDay;

    // ============================================================
    // 순서 및 타입
    // ============================================================

    /**
     * 방문 순서 (1, 2, 3...)
     */
    @Column(nullable = false)
    private Integer sequence;

    /**
     * 활동 타입 (PLACE, MEAL, ACCOMMODATION)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 20)
    private ActivityType activityType;

    // ============================================================
    // 장소 정보
    // ============================================================

    /**
     * 장소명
     * 예: "아사쿠사 센소지", "이치란 라멘"
     */
    @Column(nullable = false, length = 200)
    private String placeName;

    /**
     * Google Place ID
     */
    @Column(length = 500)
    private String placeId;

    /**
     * 주소
     */
    @Column(length = 500)
    private String address;

    // ============================================================
    // 시간 정보
    // ============================================================

    /**
     * 시작 시간 (09:30)
     */
    @Column(nullable = false)
    private LocalTime startTime;

    /**
     * 종료 시간 (11:00)
     */
    @Column(nullable = false)
    private LocalTime endTime;

    /**
     * 체류 시간 (분)
     * 예: 90분 (1시간 30분)
     */
    @Column
    private Integer durationMinutes;

    // ============================================================
    // 비용 정보
    // ============================================================

    /**
     * 입장료 (관광지만)
     */
    @Column(nullable = false)
    private Integer entranceFee = 0;

    /**
     * 식비 (식당만)
     */
    @Column(nullable = false)
    private Integer mealCost = 0;

    // ============================================================
    // 추가 정보
    // ============================================================

    /**
     * Google 평점 (4.5)
     */
    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    // ============================================================
    // 다음 장소로의 이동 정보
    // ============================================================

    /**
     * 다음 장소로 가는 교통수단
     * BUS, SUBWAY, TAXI, WALK, CAR
     */
    @Column(length = 20)
    private String transportToNext;

    /**
     * 이동 시간 (분)
     */
    @Column
    private Integer transportDuration;

    /**
     * 이동 비용
     */
    @Column(nullable = false)
    private Integer transportCost = 0;

    // ============================================================
    // 팁/조언
    // ============================================================

    /**
     * 꿀팁
     * 예: "사진 명소는 본전 왼쪽입니다"
     */
    @Column(columnDefinition = "TEXT")
    private String tips;
}