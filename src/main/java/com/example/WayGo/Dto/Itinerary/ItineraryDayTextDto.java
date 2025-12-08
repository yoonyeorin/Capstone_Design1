package com.example.WayGo.Dto.Itinerary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * TEXT 뷰에서 하루치 일정에 대한 데이터만 담는 DTO
 * - 드롭다운 제목: dayHeader
 * - 펼쳤을 때 내용: dayContent
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryDayTextDto {

    private Integer dayNumber;   // 몇 일차 (1, 2, 3...)
    private String dayHeader;    // "📅 1일차 – 12월 20일 (토)"
    private String dayContent;   // 나머지 텍스트 전체
}
