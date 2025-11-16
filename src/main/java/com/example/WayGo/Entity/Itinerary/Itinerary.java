package com.example.WayGo.Entity.Itinerary;

import com.example.WayGo.Entity.UserEntity;
import com.example.WayGo.Entity.enums.ItineraryStatus;
import com.example.WayGo.Entity.enums.ScheduleDensity;
import com.example.WayGo.Entity.enums.TravelStyle;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "itineraries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 일정 주인 (기존 UserEntity 재사용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    // 예: "도쿄 2박 3일 미식 여행"
    @Column(nullable = false, length = 200)
    private String title;

    // 도시명 (도쿄, 부산 등)
    @Column(nullable = false, length = 100)
    private String destinationCity;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    // 총 일수 (endDate - startDate + 1)
    private Integer totalDays;

    // 항공/숙소 제외 예산
    private Integer totalBudget;

    // 실제 사용된 비용 (나중에 계산해서 저장)
    private Integer totalSpent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItineraryStatus status;

    // 숙소 추천 사용 여부
    private Boolean needsAccommodation;

    @Enumerated(EnumType.STRING)
    private ScheduleDensity scheduleDensity;

    // 여행 스타일 (FOOD, CULTURE 등 여러 개)
    @ElementCollection(targetClass = TravelStyle.class)
    @CollectionTable(
            name = "itinerary_travel_styles",
            joinColumns = @JoinColumn(name = "itinerary_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "travel_style")
    private List<TravelStyle> travelStyles = new ArrayList<>();

    // createdAt / updatedAt
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) {
            this.status = ItineraryStatus.ACTIVE;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
