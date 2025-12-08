package com.example.WayGo.Service.Itinerary;

import com.example.WayGo.Constant.ErrorCode;
import com.example.WayGo.Dto.Itinerary.PlaceDto;
import com.example.WayGo.Dto.Itinerary.PlaceScoreDto;
import com.example.WayGo.Dto.Itinerary.WeatherInfoDto;
import com.example.WayGo.Exception.ItineraryException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GooglePlacesService {

    private final OkHttpClient client = new OkHttpClient();
    private final GoogleDistanceService distanceService;

    @Value("${google.maps.api.key}")
    private String GOOGLE_API_KEY;

    /**
     * 🔹 여행 스타일 -> 기본 검색 키워드
     */
    private static final Map<String, List<String>> STYLE_CODE_TO_KEYWORDS = Map.ofEntries(
            Map.entry("FOOD", List.of("restaurant", "food", "local food", "traditional restaurant")),
            Map.entry("RELAXATION", List.of("cafe", "dessert", "tea cafe", "view cafe")),
            Map.entry("ACTIVE", List.of("experience", "tour", "theme park", "activity")),
            Map.entry("NATURE", List.of("park", "lake", "river", "mountain", "botanical garden")),
            Map.entry("CULTURE", List.of("museum", "art gallery", "temple", "church", "heritage site")),
            Map.entry("CITY", List.of("tourist attraction", "famous place")),
            Map.entry("SHOPPING", List.of("shopping mall", "market", "shopping street"))
    );

    // ============================================================
    // 🔥 detectStyleTags — 자동 스타일 태깅
    // ============================================================
    private List<String> detectStyleTags(JsonObject obj, String name, String address) {

        List<String> tags = new ArrayList<>();

        // 1) Google API types 기반
        if (obj.has("types")) {
            JsonArray types = obj.getAsJsonArray("types");

            for (int i = 0; i < types.size(); i++) {
                String t = types.get(i).getAsString().toLowerCase();

                if (t.contains("restaurant")) tags.add("FOOD");
                if (t.contains("cafe") || t.contains("bakery"))
                    tags.addAll(List.of("FOOD", "RELAXATION"));

                if (t.contains("museum") || t.contains("art_gallery"))
                    tags.add("CULTURE");

                if (t.contains("church") || t.contains("temple") || t.contains("mosque"))
                    tags.add("CULTURE");

                if (t.contains("park") || t.contains("natural_feature") || t.contains("tourist_attraction"))
                    tags.add("NATURE");

                if (t.contains("shopping_mall") || t.contains("store") || t.contains("department_store"))
                    tags.add("SHOPPING");
            }
        }

        // 2) 이름 기반 보완
        String lower = name.toLowerCase();

        if (lower.contains("restaurant") || lower.contains("grill") || lower.contains("bbq"))
            tags.add("FOOD");

        if (lower.contains("cafe") || lower.contains("coffee"))
            tags.addAll(List.of("FOOD", "RELAXATION"));

        if (lower.contains("museum") || lower.contains("gallery"))
            tags.add("CULTURE");

        if (lower.contains("park"))
            tags.add("NATURE");

        // 3) 주소 기반 보완
        if (address != null) {
            String adr = address.toLowerCase();
            if (adr.contains("restaurant")) tags.add("FOOD");
            if (adr.contains("cafe")) tags.addAll(List.of("FOOD", "RELAXATION"));
            if (adr.contains("museum")) tags.add("CULTURE");
        }

        // 4) 중복 제거
        tags = new ArrayList<>(new LinkedHashSet<>(tags));

        // 5) 아무 태그 없으면 CITY
        if (tags.isEmpty()) tags.add("CITY");

        return tags;
    }

    // ============================================================
    // 🔥 Public 메서드: 장소 추천 메인 로직
    // ============================================================
    public List<PlaceDto> getPlaces(
            String city,
            Double cityLat,
            Double cityLng,
            List<String> travelStyles,
            List<WeatherInfoDto> weatherList
    ) {

        if (city == null || city.trim().isEmpty()) {
            throw new ItineraryException(ErrorCode.EMPTY_CITY);
        }

        List<PlaceDto> raw = new ArrayList<>();

        // 스타일 기본값
        if (travelStyles == null || travelStyles.isEmpty()) {
            travelStyles = List.of("CULTURE");
        }

        // 🍽️ CRITICAL: FOOD 스타일을 항상 포함 (사용자가 선택하지 않아도)
        Set<String> effectiveStyles = new LinkedHashSet<>(travelStyles);
        effectiveStyles.add("FOOD");  // ← 🔥 강제 추가!

        List<String> finalStyles = new ArrayList<>(effectiveStyles);

        int stylesCount = finalStyles.size();
        int perStyleLimit = (stylesCount <= 2) ? 8 : 5;

        // 1) 스타일별 장소 검색 (FOOD 포함)
        for (String style : finalStyles) {
            List<String> keywords = STYLE_CODE_TO_KEYWORDS.getOrDefault(style, List.of("tourist attraction"));

            for (String kw : keywords) {
                List<PlaceDto> found = searchPlaces(city, kw, perStyleLimit);
                raw.addAll(found);
            }
        }

        // 2) 중복 제거
        Map<String, PlaceDto> dedup = new LinkedHashMap<>();
        for (PlaceDto p : raw) dedup.put(p.getPlaceId(), p);
        raw = new ArrayList<>(dedup.values());

        if (raw.isEmpty()) {
            throw new ItineraryException(ErrorCode.NO_PLACES_FOUND, city + "에서 장소를 찾을 수 없습니다");
        }

        // 🍽️ FOOD 장소 개수 로깅
        long foodCount = raw.stream()
                .filter(p -> p.getStyleTags().contains("FOOD"))
                .count();
        log.info("🍽️ 검색된 FOOD 장소 수: {}", foodCount);

        // 도시 중심 좌표
        double originLat = (cityLat != null) ? cityLat : getCityCenterCoordinates(city)[0];
        double originLng = (cityLng != null) ? cityLng : getCityCenterCoordinates(city)[1];

        boolean sunny = isSunny(weatherList);

        List<PlaceScoreDto> primary = new ArrayList<>();
        List<PlaceScoreDto> backup = new ArrayList<>();

        // 3) 각 장소 거리/시간 + 점수 계산
        for (PlaceDto p : raw) {

            double straightKm = calculateStraightDistance(originLat, originLng, p.getLat(), p.getLng());
            if (straightKm > 100) continue;

            GoogleDistanceService.DistanceResult dist =
                    distanceService.getSmartDistance(originLat, originLng, p.getLat(), p.getLng(), "SUBWAY");

            int timeMin = dist.durationMinutes;
            double rating = p.getRating() != null ? p.getRating() : 0;
            double sunnyWeight = sunny ? 1.2 : 0.8;

            double timeScore = (timeMin <= 30) ? 5 :
                    (timeMin <= 60) ? 3 :
                            (timeMin <= 90) ? 1 : 0;

            double score = (rating * 2.5) + (sunnyWeight * 2.0) + timeScore - (straightKm * 0.5);

            PlaceScoreDto dto = PlaceScoreDto.builder()
                    .place(p)
                    .score(score)
                    .durationMinutes(timeMin)
                    .build();

            if (dist.statusOk && timeMin <= 60) primary.add(dto);
            else backup.add(dto);
        }

        List<PlaceScoreDto> scored = !primary.isEmpty() ? primary : backup;
        scored.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // 4) 스타일 균형 + FOOD/RELAX 적용
        return selectBalancedPlaces(scored, travelStyles, stylesCount);
    }


    // ============================================================
    // 🔥 selectBalancedPlaces — 균형 잡힌 장소 선정 (수정)
    // ============================================================
    private List<PlaceDto> selectBalancedPlaces(
            List<PlaceScoreDto> scored,
            List<String> travelStyles,
            int stylesCount
    ) {
        List<PlaceDto> selected = new ArrayList<>();

        final int MIN_STYLE = 3;
        final int MIN_FOOD = 8;  // 최소 5개 식당
        final int MIN_RELAX = travelStyles.contains("FOOD") ? 3 : 1;
        final int TARGET_TOTAL = 40;

        Map<String, Integer> count = new HashMap<>();

        // 1) FOOD 우선 확보 (식사)
        log.info("🍽️ FOOD 장소 확보 시작");
        for (PlaceScoreDto dto : scored) {
            if (selected.size() >= TARGET_TOTAL) break;
            if (count.getOrDefault("FOOD", 0) >= MIN_FOOD) break;

            if (dto.getPlace().getStyleTags().contains("FOOD")) {
                selected.add(dto.getPlace());
                count.merge("FOOD", 1, Integer::sum);
                log.info("   ✅ FOOD 추가: {} ({}개)",
                        dto.getPlace().getName(),
                        count.get("FOOD"));
            }
        }

        int finalFoodCount = count.getOrDefault("FOOD", 0);
        log.info("🍽️ 최종 확보된 FOOD 장소: {}개 (목표: {}개)", finalFoodCount, MIN_FOOD);

        // 🚨 FOOD가 부족하면 경고
        if (finalFoodCount < MIN_FOOD) {
            log.warn("⚠️ FOOD 장소가 부족합니다! 실제: {}개, 목표: {}개", finalFoodCount, MIN_FOOD);
        }

        // 2) 여행자가 선택한 스타일 확보
        for (String style : travelStyles) {
            for (PlaceScoreDto dto : scored) {
                if (selected.size() >= TARGET_TOTAL) break;
                if (count.getOrDefault(style, 0) >= MIN_STYLE) break;

                if (dto.getPlace().getStyleTags().contains(style)) {
                    if (!selected.contains(dto.getPlace())) {
                        selected.add(dto.getPlace());
                        count.merge(style, 1, Integer::sum);
                    }
                }
            }
        }

        // 3) RELAXATION (카페/디저트) 제한적 추가
        for (PlaceScoreDto dto : scored) {
            if (selected.size() >= TARGET_TOTAL) break;
            if (count.getOrDefault("RELAXATION", 0) >= MIN_RELAX) break;

            if (dto.getPlace().getStyleTags().contains("RELAXATION")) {
                if (!selected.contains(dto.getPlace())) {
                    selected.add(dto.getPlace());
                    count.merge("RELAXATION", 1, Integer::sum);
                }
            }
        }

        // 4) 남은 자리 고득점으로 채우기
        for (PlaceScoreDto dto : scored) {
            if (selected.size() >= TARGET_TOTAL) break;
            if (!selected.contains(dto.getPlace())) {
                selected.add(dto.getPlace());
            }
        }

        return selected;
    }

    // ============================================================
    // 🔽 Geocoding API
    // ============================================================
    private double[] getCityCenterCoordinates(String city) {
        try {
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String url = "https://maps.googleapis.com/maps/api/geocode/json?address="
                    + encodedCity + "&key=" + GOOGLE_API_KEY;

            Request request = new Request.Builder().url(url).build();

            try (Response res = client.newCall(request).execute()) {

                if (!res.isSuccessful()) {
                    throw new ItineraryException(ErrorCode.GOOGLE_GEOCODING_API_FAILED);
                }

                String body = res.body().string();
                JsonObject root = JsonParser.parseString(body).getAsJsonObject();

                JsonObject loc = root.getAsJsonArray("results")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("geometry")
                        .getAsJsonObject("location");

                return new double[]{
                        loc.get("lat").getAsDouble(),
                        loc.get("lng").getAsDouble()
                };
            }

        } catch (Exception e) {
            throw new ItineraryException(ErrorCode.GOOGLE_GEOCODING_API_FAILED, e);
        }
    }

    // ============================================================
    // 🔽 장소 검색 API
    // ============================================================
    private List<PlaceDto> searchPlaces(String city, String keyword, int limitCount) {
        List<PlaceDto> list = new ArrayList<>();

        try {
            String query = URLEncoder.encode(city + " " + keyword, StandardCharsets.UTF_8);

            String url =
                    "https://maps.googleapis.com/maps/api/place/textsearch/json?query="
                            + query + "&language=ko&key=" + GOOGLE_API_KEY;

            Request request = new Request.Builder().url(url).build();

            try (Response res = client.newCall(request).execute()) {

                if (!res.isSuccessful()) {
                    throw new ItineraryException(ErrorCode.GOOGLE_PLACES_API_FAILED);
                }

                String body = res.body().string();
                JsonObject root = JsonParser.parseString(body).getAsJsonObject();

                JsonArray results = root.getAsJsonArray("results");
                if (results == null || results.size() == 0) return list;

                int limit = Math.min(limitCount, results.size());

                for (int i = 0; i < limit; i++) {
                    JsonObject obj = results.get(i).getAsJsonObject();

                    String name = obj.get("name").getAsString();
                    String addr = obj.has("formatted_address")
                            ? obj.get("formatted_address").getAsString() : null;

                    JsonObject loc = obj.getAsJsonObject("geometry").getAsJsonObject("location");

                    double lat = loc.get("lat").getAsDouble();
                    double lng = loc.get("lng").getAsDouble();
                    double rating = obj.has("rating") ? obj.get("rating").getAsDouble() : 0;
                    String placeId = obj.get("place_id").getAsString();

                    // 🔥 자동 스타일 태깅
                    List<String> styleTags = detectStyleTags(obj, name, addr);

                    list.add(
                            PlaceDto.builder()
                                    .placeId(placeId)
                                    .name(name)
                                    .address(addr)
                                    .lat(lat)
                                    .lng(lng)
                                    .rating(rating)
                                    .styleTags(styleTags)
                                    .build()
                    );
                }
            }

        } catch (Exception e) {
            log.error("❌ Places 검색 오류", e);
            throw new ItineraryException(ErrorCode.GOOGLE_PLACES_API_FAILED, e);
        }

        return list;
    }

    // ============================================================
    // 기타 유틸
    // ============================================================
    private boolean isSunny(List<WeatherInfoDto> list) {
        if (list == null) return false;
        return list.stream().anyMatch(w -> "맑음".equals(w.getConditionKr()));
    }

    private double calculateStraightDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}