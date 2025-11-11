package com.example.WayGo.Dto.Itinerary;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 일정 전체 응답 DTO
 *
 * 캘린더 월별 뷰나 일정 목록에서 사용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryResponseDto {

    /**
     * 일정 ID
     */
    private Long itineraryId;

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 일정 제목
     * 예: "도쿄 2박 3일"
     */
    private String title;

    /**
     * 총 예산
     */
    private Integer totalBudget;

    /**
     * 예상 총 지출
     */
    private Integer totalSpent;

    /**
     * 상태 (ACTIVE, COMPLETED, CANCELLED)
     */
    private String status;

    /**
     * 일별 일정 목록
     */
    private List<ItineraryDayDto> days;

    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}