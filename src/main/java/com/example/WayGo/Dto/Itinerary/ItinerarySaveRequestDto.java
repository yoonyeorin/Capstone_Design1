package com.example.WayGo.Dto.Itinerary;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItinerarySaveRequestDto {

    private Long itineraryId;   // 저장하려는 일정 ID
    private Long userId;        // 로그인 유저 ID
    private boolean force;      // 겹침 시 강제 저장 여부 (팝업 YES일 때 true)
}
