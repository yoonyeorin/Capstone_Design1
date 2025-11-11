package com.example.WayGo.Entity;

import com.example.WayGo.Entity.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 여행 일정 생성을 위한 사용자 입력 데이터를 저장하는 Entity
 *
 * ⚠️ ID 타입: Integer (기존 users, posts 테이블과 통일)
 */
@Entity
@Table(name = "itinerary_inputs")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class ItineraryInput {

    // ============================================================
    // 기본 PK (Primary Key) - Long → Integer로 변경!
    // ============================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;  // ⬅️ Long에서 Integer로 변경

    // ============================================================
    // 사용자 연관관계
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // ============================================================
    // 1단계: 도시 정보
    // ============================================================

    @Column(nullable = false, length = 255)
    private String destinationCity;

    @Column(nullable = false, length = 500)
    private String destinationPlaceId;

    // ============================================================
    // 2단계: 날짜 정보
    // ============================================================

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer totalDays;

    // ============================================================
    // 3단계: 교통편 정보
    // ============================================================

    @Column(nullable = false)
    private Boolean hasTransportTicket = false;

    private LocalTime arrivalTime;

    private LocalTime departureTime;

    // ============================================================
    // 4단계: 인원수
    // ============================================================

    @Column(nullable = false)
    private Integer numberOfPeople;

    // ============================================================
    // 5단계: 이동수단
    // ============================================================

    @ElementCollection
    @CollectionTable(
            name = "itinerary_transport_types",
            joinColumns = @JoinColumn(name = "itinerary_input_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type")
    private List<TransportType> transportTypes = new ArrayList<>();

    // ============================================================
    // 6-1단계: 여행 취향
    // ============================================================

    @ElementCollection
    @CollectionTable(
            name = "itinerary_travel_styles",
            joinColumns = @JoinColumn(name = "itinerary_input_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "travel_style")
    private List<TravelStyle> travelStyles = new ArrayList<>();

    // ============================================================
    // 6-2단계: 일정 밀도
    // ============================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleDensity scheduleDensity;

    // ============================================================
    // 7단계: 예산
    // ============================================================

    @Column(nullable = false)
    private Integer budget;

    // ============================================================
    // 8단계: 숙소 추천
    // ============================================================

    @Column(nullable = false)
    private Boolean needsAccommodation = false;

    private Integer accommodationBudget;

    // ============================================================
    // 상태 관리
    // ============================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InputStatus status = InputStatus.IN_PROGRESS;

    // ============================================================
    // 감사(Audit) 필드
    // ============================================================

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ============================================================
    // 생성자
    // ============================================================

    public ItineraryInput() {
        this.transportTypes = new ArrayList<>();
        this.travelStyles = new ArrayList<>();
        this.status = InputStatus.IN_PROGRESS;
        this.hasTransportTicket = false;
        this.needsAccommodation = false;
    }
}