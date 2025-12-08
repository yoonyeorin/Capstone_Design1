package com.example.WayGo.Dto.Itinerary;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ItineraryGenerationFormDto {

    @Schema(description = "여행 도시", example = "도쿄")
    private String destinationCity;

    @Schema(description = "여행 시작일", example = "2025-12-20")
    private String startDate;

    @Schema(description = "여행 종료일", example = "2025-12-23")
    private String endDate;

    @Schema(description = "항공권을 이미 예매했는지 여부", example = "false")
    private Boolean hasTransportTicket;

    @Schema(description = "여행지 도착 예상 시간(HH:mm)", example = "09:00")
    private String expectedArrivalTime;

    @Schema(description = "귀가 예상 시간(HH:mm)", example = "18:00")
    private String expectedReturnTime;

    @Schema(description = "여행 인원", example = "2")
    private Integer travelers;

    @Schema(description = "이동수단 목록", example = "[\"지하철\", \"도보\"]")
    private List<String> transportTypes;

    @Schema(description = "여행 스타일", example = "[\"미식 여행 스타일\", \"문화/역사 탐방 스타일\"]")
    private List<String> travelStyles;

    @Schema(description = "일정 밀도", example = "느슨한 계획")
    private String scheduleDensity;

    @Schema(description = "전체 예산(비행기/숙소 제외, 원화)", example = "1500000")
    private Integer budget;

    @Schema(description = "숙소 추천 필요 여부", example = "true")
    private Boolean needsHotel;

    @Schema(description = "1박 숙소 예산(원화)", example = "100000")
    private Integer hotelBudget;

    @Schema(description = "목적지 위도", example = "35.6895")
    private Double destLat;

    @Schema(description = "목적지 경도", example = "139.6917")
    private Double destLng;
}
