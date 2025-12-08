package com.example.WayGo.Dto.Itinerary;

import lombok.*;
import java.util.Map;

@Builder
@Getter
@AllArgsConstructor
public class ItineraryAiResult {
    private String text;  // ---TEXT--- 전체
    private String json;  // ---JSON---
    private Map<Integer, String> dayTexts; // 1일차, 2일차…
}
