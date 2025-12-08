package com.example.WayGo.Dto.Itinerary;

import lombok.*;

/**
 * AI가 생성한 원본 일정 텍스트 응답 DTO
 * - 프론트에서 "그냥 그대로 보여주기용"
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryAiRawResponseDto {

    private Long itineraryId;    // 일정 ID
    private String aiRawText;    // OpenAI가 생성한 그대로의 문자열
}
