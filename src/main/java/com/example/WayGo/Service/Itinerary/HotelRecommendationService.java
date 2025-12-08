package com.example.WayGo.Service.Itinerary;

import com.example.WayGo.Dto.Itinerary.HotelRecommendationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelRecommendationService {

    @Value("${google.maps.api.key}")
    private String API_KEY;

    private final RestTemplate restTemplate;

    /**
     * 🔵 기존: 도시 이름 기반 호텔 추천 (fallback 용)
     */
    public List<HotelRecommendationDto> getHotels(String city, int hotelBudget) {

        List<HotelRecommendationDto> hotels = new ArrayList<>();

        try {
            // 영문 도시명 + "hotel" 검색
            String query = URLEncoder.encode(city + " hotel", StandardCharsets.UTF_8);

            String url = "https://maps.googleapis.com/maps/api/place/textsearch/json" +
                    "?query=" + query +
                    "&language=ko" +
                    "&key=" + API_KEY;

            log.info("🏨 [TEXTSEARCH] 호텔 검색 시작: {} (예산: {}원)", city, hotelBudget);

            Map response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                log.warn("⚠️ Hotel Places 응답 null");
                return hotels;
            }

            String status = (String) response.get("status");
            if ("ZERO_RESULTS".equals(status)) {
                log.warn("⚠️ '{}' 검색 결과 0개 - 다른 키워드 시도", city);

                // 대체 검색: accommodation
                return searchHotelsWithKeyword(city + " accommodation", hotelBudget);
            }

            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) response.get("results");

            if (results == null || results.isEmpty()) {
                log.warn("⚠️ Hotel Places 결과 0개");
                return hotels;
            }

            hotels = parseHotelResults(results, hotelBudget);

            // 평점 순 정렬
            hotels.sort(Comparator
                    .comparing(HotelRecommendationDto::getRating, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(HotelRecommendationDto::getName, Comparator.nullsLast(String::compareTo)));

            if (hotels.size() > 3) {
                hotels = hotels.subList(0, 3);
            }

            log.info("🎯 [TEXTSEARCH] 최종 추천 호텔 개수: {}", hotels.size());
            return hotels;

        } catch (Exception e) {
            log.error("❌ Hotel Places 호출 중 예외 발생", e);
            return hotels;
        }
    }

    /**
     * 🟢 신규: Day1 마지막 장소 좌표 기준 호텔 추천
     * - 주변 2km 내 lodging 3개
     */
    public List<HotelRecommendationDto> getHotelsNear(double lat, double lng, int hotelBudget) {

        List<HotelRecommendationDto> hotels = new ArrayList<>();

        try {
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                    "?location=" + lat + "," + lng +
                    "&radius=2000" +          // 2km 반경
                    "&type=lodging" +         // 숙박 시설
                    "&language=ko" +
                    "&key=" + API_KEY;

            log.info("🏨 [NEARBY] 좌표 기준 호텔 검색: lat={}, lng={}, 예산={}원", lat, lng, hotelBudget);

            Map response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                log.warn("⚠️ Nearby Hotel 응답 null");
                return hotels;
            }

            String status = (String) response.get("status");
            if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                log.warn("⚠️ Nearby Hotel API 상태 이상: {}", status);
                return hotels;
            }

            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) response.get("results");

            if (results == null || results.isEmpty()) {
                log.warn("⚠️ Nearby Hotel 결과 0개");
                return hotels;
            }

            hotels = parseHotelResults(results, hotelBudget);

            // 평점 순 정렬
            hotels.sort(Comparator
                    .comparing(HotelRecommendationDto::getRating, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(HotelRecommendationDto::getName, Comparator.nullsLast(String::compareTo)));

            if (hotels.size() > 3) {
                hotels = hotels.subList(0, 3);
            }

            log.info("🎯 [NEARBY] 최종 추천 호텔 개수: {}", hotels.size());
            return hotels;

        } catch (Exception e) {
            log.error("❌ Nearby Hotel 호출 중 예외 발생", e);
            return hotels;
        }
    }

    /**
     * ⭐ 대체 키워드로 호텔 검색 (city + accommodation)
     */
    private List<HotelRecommendationDto> searchHotelsWithKeyword(String searchQuery, int hotelBudget) {
        List<HotelRecommendationDto> hotels = new ArrayList<>();

        try {
            String query = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String url = "https://maps.googleapis.com/maps/api/place/textsearch/json" +
                    "?query=" + query +
                    "&language=ko" +
                    "&key=" + API_KEY;

            log.info("🏨 [TEXTSEARCH] 대체 검색 시도: {}", searchQuery);

            Map response = restTemplate.getForObject(url, Map.class);
            if (response == null) return hotels;

            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) response.get("results");

            if (results == null || results.isEmpty()) {
                log.warn("⚠️ 대체 검색도 결과 0개");
                return hotels;
            }

            hotels = parseHotelResults(results, hotelBudget);

            if (hotels.size() > 3) {
                hotels = hotels.subList(0, 3);
            }

            log.info("🎯 대체 검색 결과: {}개", hotels.size());
            return hotels;

        } catch (Exception e) {
            log.error("❌ 대체 검색 중 예외", e);
            return hotels;
        }
    }

    /**
     * ⭐ 공통 파싱 로직 (TextSearch / NearbySearch 모두 사용)
     */
    @SuppressWarnings("unchecked")
    private List<HotelRecommendationDto> parseHotelResults(List<Map<String, Object>> results, int hotelBudget) {
        List<HotelRecommendationDto> hotels = new ArrayList<>();

        int maxPriceLevel = (hotelBudget > 0) ? mapBudgetToPriceLevel(hotelBudget) : 4;

        log.info("🏨 호텔 파싱 시작: {}개, 예산: {}, 허용 price_level: <= {}",
                results.size(), hotelBudget, maxPriceLevel);

        for (Map<String, Object> r : results) {

            double rating = 0.0;
            Object ratingObj = r.get("rating");
            if (ratingObj instanceof Number) {
                rating = ((Number) ratingObj).doubleValue();
            }

            int priceLevel = 2;
            Object priceObj = r.get("price_level");
            if (priceObj instanceof Number) {
                priceLevel = ((Number) priceObj).intValue();
            }

            if (hotelBudget > 0 && priceLevel > maxPriceLevel) {
                continue;
            }

            Map<String, Object> geometry = (Map<String, Object>) r.get("geometry");
            if (geometry == null) continue;

            Map<String, Object> location = (Map<String, Object>) geometry.get("location");
            if (location == null) continue;

            double lat = 0.0;
            double lng = 0.0;

            Object latObj = location.get("lat");
            Object lngObj = location.get("lng");

            if (latObj instanceof Number) lat = ((Number) latObj).doubleValue();
            if (lngObj instanceof Number) lng = ((Number) lngObj).doubleValue();

            String name = (String) r.get("name");

            // nearbysearch는 formatted_address 대신 vicinity가 올 수 있음
            String address = null;
            Object formattedAddress = r.get("formatted_address");
            Object vicinity = r.get("vicinity");
            if (formattedAddress instanceof String fa) {
                address = fa;
            } else if (vicinity instanceof String vc) {
                address = vc;
            }

            Integer estimatedPrice = estimatePriceFromLevel(priceLevel, hotelBudget);

            hotels.add(
                    HotelRecommendationDto.builder()
                            .name(name)
                            .address(address)
                            .rating(rating)
                            .price(estimatedPrice)
                            .currency("KRW")
                            .lat(lat)
                            .lng(lng)
                            .build()
            );
        }

        return hotels;
    }

    /**
     * price_level → 대략적인 가격 추정
     */
    private Integer estimatePriceFromLevel(int priceLevel, int userBudget) {
        Map<Integer, int[]> priceLevelRanges = Map.of(
                0, new int[]{30000, 50000},
                1, new int[]{50000, 80000},
                2, new int[]{80000, 120000},
                3, new int[]{120000, 200000},
                4, new int[]{200000, 400000}
        );

        int[] range = priceLevelRanges.getOrDefault(priceLevel, new int[]{80000, 120000});

        if (userBudget > 0) {
            int minPrice = Math.min(range[0], userBudget);
            int maxPrice = Math.min(range[1], userBudget);
            return (minPrice + maxPrice) / 2;
        }

        return (range[0] + range[1]) / 2;
    }

    /**
     * 사용자가 입력한 1박 예산 → price_level 상한
     */
    private int mapBudgetToPriceLevel(int budget) {
        if (budget <= 50000) return 1;
        else if (budget <= 100000) return 2;
        else if (budget <= 150000) return 3;
        else return 4;
    }
}
