package com.example.WayGo.Dto.Itinerary;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItinerarySaveResponseDto {

    private String status;              // OK / OVERLAP / SAVED
    private String message;             // 사용자 안내 메시지
    private Long savedItineraryId;      // 최종 저장된 일정 ID
    private List<Long> deletedIds;      // 삭제된 기존 일정 ID 목록 (force=true일 때)
}
