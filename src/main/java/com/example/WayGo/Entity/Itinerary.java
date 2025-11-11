package com.example.WayGo.Entity;

import com.example.WayGo.Entity.enums.ItineraryStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 일정 메타데이터 Entity
 *
 * 전체 여행의 요약 정보를 저장
 * - 제목: "도쿄 2박 3일"
 * - 예산 관리
 * - 상태 관리
 */
@Entity
@Table(name = "itineraries")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Itinerary {

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
     * 어떤 입력으로 생성됐는지
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_input_id", nullable = false)
    private ItineraryInput itineraryInput;

    /**
     * 누구의 일정인지
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /**
     * 일별 일정 목록
     */
    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItineraryDay> days = new ArrayList<>();

    // ============================================================
    // 기본 정보
    // ============================================================

    /**
     * 일정 제목 (자동 생성)
     * 예: "도쿄 2박 3일"
     */
    @Column(nullable = false, length = 200)
    private String title;

    // ============================================================
    // 예산 관리
    // ============================================================

    /**
     * 총 예산 (입력받은 예산)
     */
    @Column(nullable = true)
    private Integer totalBudget;

    /**
     * 예상 총 지출 (알고리즘이 계산)
     */
    @Column(nullable = false)
    private Integer totalSpent = 0;

    // ============================================================
    // 상태 관리
    // ============================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItineraryStatus status = ItineraryStatus.ACTIVE;

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
    // 연관관계 편의 메서드
    // ============================================================

    /**
     * 일별 일정 추가
     */
    public void addDay(ItineraryDay day) {
        this.days.add(day);
        day.setItinerary(this);
    }

    /**
     * 일별 일정 제거
     */
    public void removeDay(ItineraryDay day) {
        this.days.remove(day);
        day.setItinerary(null);
    }
}