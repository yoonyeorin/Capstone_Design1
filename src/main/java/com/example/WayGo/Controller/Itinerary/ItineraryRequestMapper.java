package com.example.WayGo.Controller.Itinerary;

import com.example.WayGo.Dto.Itinerary.ItineraryGenerationFormDto;
import com.example.WayGo.Dto.Itinerary.ItineraryGenerationRequestDto;
import com.example.WayGo.Entity.enums.ScheduleDensity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ItineraryRequestMapper {

    // 🔹 이동수단 매핑 (한글 → 내부 코드)
    private static final Map<String, String> KOR_TO_TRANSPORT = Map.ofEntries(
            Map.entry("지하철", "SUBWAY"),
            Map.entry("버스", "BUS"),
            Map.entry("택시", "TAXI"),
            Map.entry("걷기", "WALK"),
            Map.entry("도보", "WALK"),
            Map.entry("자차", "CAR"),
            Map.entry("렌터카", "CAR")
    );

    // 🔹 일정 밀도 (한글 → Enum)
    private static final Map<String, ScheduleDensity> KOR_TO_DENSITY = Map.of(
            "느슨한 계획", ScheduleDensity.RELAXED,
            "빡빡한 계획", ScheduleDensity.PACKED
    );

    // 🔹 여행 스타일 (프론트 한글 라벨 → 내부 코드)
    private static final Map<String, String> KOR_TO_TRAVEL_STYLE = Map.ofEntries(
            Map.entry("활동적인 스타일", "ACTIVE"),
            Map.entry("차분한 휴양 스타일", "RELAXATION"),
            Map.entry("자연 탐방 스타일", "NATURE"),
            Map.entry("문화/역사 탐방 스타일", "CULTURE"),
            Map.entry("미식 여행 스타일", "FOOD"),
            Map.entry("도시 탐험 스타일", "CITY")   // 아래 GooglePlacesService 에 CITY 추가해줄 것
    );

    public ItineraryGenerationRequestDto toGenerationRequest(ItineraryGenerationFormDto form) {

        // 1) 이동수단 매핑 (한글 → 코드)
        List<String> transportTypes = null;
        if (form.getTransportTypes() != null) {
            transportTypes = form.getTransportTypes().stream()
                    .map(label -> KOR_TO_TRANSPORT.getOrDefault(label, label)) // 이미 "SUBWAY"면 그대로 통과
                    .collect(Collectors.toList());
        }

        // 2) 여행 스타일 매핑 (한글 → 코드)
        List<String> travelStyles = null;
        if (form.getTravelStyles() != null) {
            travelStyles = form.getTravelStyles().stream()
                    .map(label -> KOR_TO_TRAVEL_STYLE.getOrDefault(label, label)) // 이미 "FOOD"면 그대로 통과
                    .collect(Collectors.toList());
        }

        // 3) 일정 밀도 매핑 (한글 or 코드 → Enum)
        ScheduleDensity density = null;
        if (form.getScheduleDensity() != null && !form.getScheduleDensity().isBlank()) {
            // 우선 한글 라벨 매핑 시도
            density = KOR_TO_DENSITY.get(form.getScheduleDensity());
            // 안 맞으면 그대로 Enum 이름으로도 시도 ("RELAXED", "PACKED")
            if (density == null) {
                try {
                    density = ScheduleDensity.valueOf(form.getScheduleDensity().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        // 4) 위도/경도 (그대로 전달)
        Double destLat = form.getDestLat();
        Double destLng = form.getDestLng();

        return ItineraryGenerationRequestDto.builder()
                .destinationCity(form.getDestinationCity())
                .startDate(form.getStartDate())
                .endDate(form.getEndDate())
                .hasTransportTicket(form.getHasTransportTicket())
                .expectedArrivalTime(form.getExpectedArrivalTime())
                .expectedReturnTime(form.getExpectedReturnTime())
                .travelers(form.getTravelers())
                .transportTypes(transportTypes)
                .travelStyles(travelStyles)
                .scheduleDensity(density)
                .budget(form.getBudget())
                .needsHotel(form.getNeedsHotel())
                .hotelBudget(form.getHotelBudget())
                .destLat(destLat)
                .destLng(destLng)
                .build();
    }
}
