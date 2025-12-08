package com.example.WayGo.Dto.Itinerary;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OverlapCheckResponseDto {

    private boolean overlap;   // true=겹침
    private Long existingItineraryId;
    private String existingTitle;
    private String existingStart;
    private String existingEnd;
}
