package com.example.WayGo.Dto.Itinerary;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 일정 간단 요약 DTO
 *
 * 캘린더 월별 뷰에서 사용
 * 각 날짜에 주요 랜드마크만 표시
 *
 * ┌─────────────┐
 * │     [9]     │
 * │   --------  │
 * │   ·센소지   │
 * │   ·도쿄타워 │
 * └─────────────┘
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItinerarySummaryDto {

    /**
     * 일정 ID
     */
    private Long itineraryId;

    /**
     * 제목 (예: "도쿄 2박 3일")
     */
    private String title;

    /**
     * 시작 날짜
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 종료 날짜
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 총 일수
     */
    private Integer totalDays;

    /**
     * 주요 랜드마크 목록 (최대 4개)
     */
    private List<String> mainPlaces;

    /**
     * 상태
     */
    private String status;
}