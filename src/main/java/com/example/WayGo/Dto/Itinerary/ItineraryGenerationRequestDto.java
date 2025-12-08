package com.example.WayGo.Dto.Itinerary;

import com.example.WayGo.Entity.enums.ScheduleDensity;
import lombok.*;

import java.util.List;

/**
 * 일정 생성 요청 DTO (Step1~8 전체 포함)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryGenerationRequestDto {

    private String destinationCity;
    private String startDate;
    private String endDate;
    private Boolean hasTransportTicket;
    private String expectedArrivalTime;
    private String expectedReturnTime;
    private Integer travelers;
    private List<String> transportTypes;
    private List<String> travelStyles;
    private ScheduleDensity scheduleDensity;
    private Integer budget;
    private Boolean needsHotel;
    private Integer hotelBudget;

    private Double destLat;
    private Double destLng;
}
