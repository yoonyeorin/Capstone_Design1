package com.example.WayGo.Dto.Itinerary;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryInputResponseDto {

    private Integer inputId;      // ✅ Integer (itinerary_inputs 테이블)
    private Long userId;          // ✅ Long (users 테이블) ⬅️ 여기만 Long으로!
    private String status;

    private String destinationCity;
    private String destinationPlaceId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Integer totalDays;

    private Boolean hasTransportTicket;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime arrivalTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime departureTime;

    private Integer numberOfPeople;

    private List<String> transportTypes;

    private List<String> travelStyles;
    private String scheduleDensity;

    private Integer budget;

    private Boolean needsAccommodation;
    private Integer accommodationBudget;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}