package com.example.WayGo.Dto.Itinerary;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Google Places API 개별 검색 결과
 */
@Getter
@Setter
@NoArgsConstructor
public class GooglePlaceResultDto {

    @JsonProperty("place_id")
    private String placeId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("formatted_address")
    private String formattedAddress;

    @JsonProperty("geometry")
    private Geometry geometry;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Geometry {

        @JsonProperty("location")
        private Location location;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Location {

        @JsonProperty("lat")
        private Double lat;

        @JsonProperty("lng")
        private Double lng;
    }
}