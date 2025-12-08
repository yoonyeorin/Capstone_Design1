package com.example.WayGo.Dto.Itinerary;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryCalendarSummaryDto {

    private String date;              // "2025-11-09"
    private List<String> mainPlaces;  // ["센소지", "도쿄타워"]
}
