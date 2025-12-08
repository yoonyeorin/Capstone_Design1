package com.example.WayGo.Entity.Itinerary;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;
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

    // 날씨 요약
    private String weatherCondition;

    private Integer temperature;

    // "모자를 챙겨주세요" 같은 요약 문구
    @Column(length = 500)
    private String weatherAdvice;

    // 하루 예산
    private Integer dailyBudget;
    private Integer dailySpent;

    // ✨ 새로 추가: 헤더 (드롭다운 제목용)
    @Column(length = 500)
    private String dayHeader;  // 예: "📅 1일차 – 12월 15일 (월)"

    // ✨ 새로 추가: 내용 (드롭다운 본문용)
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String dayContent;  // 헤더 제외한 나머지 모든 내용

    // 기존: 전체 텍스트 (호환성 유지)
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String dayText;  // 전체 텍스트 (기존 유지)

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String dayTextFull;

    @OneToMany(mappedBy = "itineraryDay", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ItineraryActivity> activities;
}