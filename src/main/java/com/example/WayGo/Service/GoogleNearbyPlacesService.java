package com.example.WayGo.Service;

import com.example.WayGo.Dto.PlaceDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleNearbyPlacesService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.api.key}")
    private String apiKey;

    private static final String PLACES_API_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";

    // ⭐ 카테고리별 Google Places API 표준 타입 매핑
    private static final Map<String, String> TYPE_MAP = new HashMap<>() {{
        put("restroom", "toilet");
        put("hospital", "hospital");
        put("convenience_store", "convenience_store");
        put("gas_station", "gas_station");
    }};

    /**
     * 주변 장소 검색 (개선 버전)
     */
    public List<PlaceDTO> searchNearbyPlaces(Double latitude, Double longitude, String category, Integer radius) {
        try {
            // ⭐ 화장실의 경우 키워드 검색 + 타입 검색 병행
            if ("restroom".equals(category)) {
                return searchRestrooms(latitude, longitude, radius);
            }

            // 나머지 카테고리는 기존 타입 검색 방식 사용
            return searchByType(latitude, longitude, category, radius);

        } catch (Exception e) {
            log.error("장소 검색 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("장소 검색에 실패했습니다.", e);
        }
    }

    /**
     * 화장실 전용 검색 (키워드 + 타입 검색 병행)
     */
    private List<PlaceDTO> searchRestrooms(Double latitude, Double longitude, Integer radius) {
        log.info("화장실 검색 시작: lat={}, lng={}, radius={}m", latitude, longitude, radius);

        List<PlaceDTO> allPlaces = new ArrayList<>();

        // 1. 키워드 검색: "화장실"
        List<PlaceDTO> keywordResults = searchByKeyword(latitude, longitude, "화장실", radius);
        log.info("키워드 '화장실' 검색 결과: {} 개", keywordResults.size());
        allPlaces.addAll(keywordResults);

        // 2. 키워드 검색: "공중화장실"
        List<PlaceDTO> publicToiletResults = searchByKeyword(latitude, longitude, "공중화장실", radius);
        log.info("키워드 '공중화장실' 검색 결과: {} 개", publicToiletResults.size());
        allPlaces.addAll(publicToiletResults);

        // 3. 타입 검색: toilet (호텔, 카페 등의 화장실)
        List<PlaceDTO> typeResults = searchByType(latitude, longitude, "restroom", radius);
        log.info("타입 'toilet' 검색 결과: {} 개", typeResults.size());
        allPlaces.addAll(typeResults);

        // 4. 중복 제거 (placeId 기준)
        Map<String, PlaceDTO> uniquePlaces = new HashMap<>();
        for (PlaceDTO place : allPlaces) {
            uniquePlaces.putIfAbsent(place.getPlaceId(), place);
        }

        List<PlaceDTO> results = new ArrayList<>(uniquePlaces.values());

        // 5. 거리순 정렬
        results.sort(Comparator.comparingDouble(PlaceDTO::getDistance));

        log.info("최종 화장실 검색 결과: {} 개 (중복 제거 후)", results.size());
        return results;
    }

    /**
     * 키워드 기반 검색
     */
    private List<PlaceDTO> searchByKeyword(Double latitude, Double longitude, String keyword, Integer radius) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(PLACES_API_URL)
                    .queryParam("location", latitude + "," + longitude)
                    .queryParam("radius", radius)
                    .queryParam("keyword", keyword)  // ⭐ 키워드 검색
                    .queryParam("key", apiKey)
                    .queryParam("language", "ko")
                    .toUriString();

            log.debug("키워드 검색 API 호출: keyword={}", keyword);

            String response = restTemplate.getForObject(url, String.class);
            return parseApiResponse(response, "restroom", latitude, longitude);

        } catch (Exception e) {
            log.error("키워드 검색 중 오류: keyword={}, error={}", keyword, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 타입 기반 검색 (기존 방식)
     */
    private List<PlaceDTO> searchByType(Double latitude, Double longitude, String category, Integer radius) {
        try {
            String placeType = TYPE_MAP.getOrDefault(category, "toilet");

            String url = UriComponentsBuilder.fromHttpUrl(PLACES_API_URL)
                    .queryParam("location", latitude + "," + longitude)
                    .queryParam("radius", radius)
                    .queryParam("type", placeType)
                    .queryParam("key", apiKey)
                    .queryParam("language", "ko")
                    .toUriString();

            log.info("타입 검색 API 호출: category={}, type={}, radius={}m", category, placeType, radius);

            String response = restTemplate.getForObject(url, String.class);
            return parseApiResponse(response, category, latitude, longitude);

        } catch (Exception e) {
            log.error("타입 검색 중 오류: category={}, error={}", category, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * API 응답 파싱 (공통 로직)
     */
    private List<PlaceDTO> parseApiResponse(String response, String category, Double userLat, Double userLng) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            String status = rootNode.get("status").asText();

            if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                String errorMessage = rootNode.has("error_message")
                        ? rootNode.get("error_message").asText()
                        : status;
                log.error("Google Places API 오류: {} - {}", status, errorMessage);
                return new ArrayList<>();
            }

            if ("ZERO_RESULTS".equals(status)) {
                return new ArrayList<>();
            }

            List<PlaceDTO> places = new ArrayList<>();
            JsonNode results = rootNode.get("results");

            for (JsonNode result : results) {
                String address = result.has("formatted_address")
                        ? result.get("formatted_address").asText()
                        : (result.has("vicinity") ? result.get("vicinity").asText() : "주소 정보 없음");

                PlaceDTO place = PlaceDTO.builder()
                        .placeId(result.get("place_id").asText())
                        .name(result.get("name").asText())
                        .latitude(result.get("geometry").get("location").get("lat").asDouble())
                        .longitude(result.get("geometry").get("location").get("lng").asDouble())
                        .address(address)
                        .category(category)
                        .rating(result.has("rating") ? result.get("rating").asDouble() : null)
                        .openNow(result.has("opening_hours") && result.get("opening_hours").has("open_now")
                                ? result.get("opening_hours").get("open_now").asBoolean()
                                : null)
                        .build();

                // 거리 계산
                double distance = calculateDistance(userLat, userLng, place.getLatitude(), place.getLongitude());
                place.setDistance(distance);

                places.add(place);
            }

            return places;

        } catch (Exception e) {
            log.error("API 응답 파싱 중 오류: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 두 좌표 간의 거리 계산 (Haversine formula)
     * @return 거리 (미터)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371; // 지구 반지름 (km)

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c * 1000; // km를 m로 변환
    }

    /**
     * 카테고리 유효성 검사
     */
    public boolean isValidCategory(String category) {
        return TYPE_MAP.containsKey(category);
    }
}