package com.example.WayGo.Service.Itinerary;

import com.example.WayGo.Dto.Itinerary.GooglePlaceResultDto;
import com.example.WayGo.Dto.Itinerary.GooglePlacesApiResponseDto;
import com.example.WayGo.Dto.Itinerary.Step1ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Google Places API 호출 서비스 (API Key 방식)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GooglePlacesService {

    private final RestTemplate restTemplate;

    /**
     * Google API Key
     *
     * ⚠️ Places API는 API Key 방식을 사용합니다!
     * (서비스 계정 JSON은 Translation, Vision 등에서만 사용)
     */
    @Value("${google.api.key}")
    private String apiKey;

    private static final String PLACES_API_URL =
            "https://maps.googleapis.com/maps/api/place/textsearch/json";

    public List<Step1ResponseDto> searchCity(String cityName) {
        log.info("도시 검색 시작: {}", cityName);

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(PLACES_API_URL)
                    .queryParam("query", cityName)
                    .queryParam("type", "locality")
                    .queryParam("key", apiKey)
                    .build()
                    .toUriString();

            log.debug("Google Places API 호출");

            GooglePlacesApiResponseDto response = restTemplate.getForObject(
                    url,
                    GooglePlacesApiResponseDto.class
            );

            if (response == null || response.getResults() == null) {
                log.warn("Google API 응답이 null입니다");
                return new ArrayList<>();
            }

            if (!"OK".equals(response.getStatus())) {
                log.error("Google API 에러: {}", response.getStatus());
                if (response.getErrorMessage() != null) {
                    log.error("에러 메시지: {}", response.getErrorMessage());
                }
                return new ArrayList<>();
            }

            List<Step1ResponseDto> results = response.getResults().stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            log.info("검색 완료: {}개 결과 반환", results.size());
            return results;

        } catch (Exception e) {
            log.error("도시 검색 중 에러 발생", e);
            throw new RuntimeException("도시 검색 실패: " + e.getMessage(), e);
        }
    }

    private Step1ResponseDto convertToDto(GooglePlaceResultDto result) {
        return Step1ResponseDto.builder()
                .placeId(result.getPlaceId())
                .cityName(result.getName())
                .formattedAddress(result.getFormattedAddress())
                .build();
    }
}