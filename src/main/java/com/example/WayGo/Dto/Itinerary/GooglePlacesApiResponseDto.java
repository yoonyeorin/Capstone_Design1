package com.example.WayGo.Dto.Itinerary;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Google Places API 전체 응답
 * Service에서만 사용 (내부용)
 */
@Getter
@Setter
@NoArgsConstructor
public class GooglePlacesApiResponseDto {

    @JsonProperty("results")
    private List<GooglePlaceResultDto> results;

    @JsonProperty("status")
    private String status;

    @JsonProperty("error_message")
    private String errorMessage;
}