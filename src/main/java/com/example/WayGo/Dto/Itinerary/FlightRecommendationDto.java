package com.example.WayGo.Dto.Itinerary;

import lombok.*;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlightRecommendationDto {
    private String flightNumber;
    private String airline;

    // 출발/도착 시각
    private LocalDateTime departure;
    private LocalDateTime arrival;

    // 가격 정보
    private Integer price;
    private String currency;
    // 직항 여부
    private Boolean isDirect;
    private String duration;          // "2시간 30분" 같은 사람 읽는 문자열

    // 🔹 OpenAI 프롬프트에서 쓰는 필드들 추가
    private Integer durationMinutes;  // getDurationMinutes()
    private String departureAirport;  // getDepartureAirport()
    private String arrivalAirport;    // getArrivalAirport()
    private String departureTime;     // getDepartureTime() → "06:00" 같은 표현
    private String arrivalTime;
}
