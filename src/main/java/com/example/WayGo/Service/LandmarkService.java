package com.example.WayGo.Service;

import com.example.WayGo.Dto.LandmarkDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LandmarkService {

    private final RestTemplate restTemplate;

    @Value("${google.places.api.key}")
    private String googlePlacesApiKey;

    /**
     * 도시명으로 유명 랜드마크 Top 3 조회 (신규 Places API 사용)
     */
    public List<LandmarkDTO> getTopLandmarks(String cityName) {
        try {
            // Google Places API (New) - Text Search
            String url = "https://places.googleapis.com/v1/places:searchText";

            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Goog-Api-Key", googlePlacesApiKey);
            headers.set("X-Goog-FieldMask", "places.displayName,places.formattedAddress,places.rating,places.photos");

            // 요청 본문 설정
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("textQuery", "famous landmarks in " + cityName);
            requestBody.put("languageCode", "ko");
            requestBody.put("maxResultCount", 3);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.info("Google Places API 호출 - 도시: {}", cityName);

            // API 호출
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            log.info("Google Places API 응답: {}", responseBody);

            if (responseBody != null && responseBody.containsKey("places")) {
                List<Map<String, Object>> places = (List<Map<String, Object>>) responseBody.get("places");

                if (places != null && !places.isEmpty()) {
                    return places.stream()
                            .limit(3)
                            .map(this::convertToDTO)
                            .collect(Collectors.toList());
                }
            }

            // 빈 데이터인 경우
            log.warn("Google Places API 응답 데이터가 비어있습니다. 도시: {}", cityName);
            throw new RuntimeException("해당 도시의 랜드마크를 찾을 수 없습니다.");

        } catch (RuntimeException e) {
            // 이미 던져진 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            // API 호출 실패
            log.error("Google Places API 호출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("랜드마크 정보를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * Google Places (New) 응답을 DTO로 변환
     */
    private LandmarkDTO convertToDTO(Map<String, Object> place) {
        // displayName 추출
        Map<String, Object> displayName = (Map<String, Object>) place.get("displayName");
        String name = displayName != null ? (String) displayName.get("text") : "Unknown";

        // 주소 추출
        String address = (String) place.get("formattedAddress");

        // 평점 추출
        Double rating = place.get("rating") != null ?
                ((Number) place.get("rating")).doubleValue() : 0.0;

        // 사진 URL 추출 (신규 API 방식)
        String photoUrl = null;
        if (place.containsKey("photos")) {
            List<Map<String, Object>> photos = (List<Map<String, Object>>) place.get("photos");
            if (!photos.isEmpty()) {
                String photoName = (String) photos.get(0).get("name");
                if (photoName != null) {
                    // 사진 이름을 URL로 변환
                    photoUrl = String.format(
                            "https://places.googleapis.com/v1/%s/media?maxWidthPx=400&key=%s",
                            photoName, googlePlacesApiKey
                    );
                }
            }
        }

        return LandmarkDTO.builder()
                .landmarkName(name)
                .description("")
                .imageUrl(photoUrl)
                .rating(rating)
                .address(address)
                .build();
    }
}