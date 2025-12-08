package com.example.WayGo.Service.Itinerary;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleDistanceService {

    private final OkHttpClient client = new OkHttpClient();

    @Value("${google.maps.api.key}")
    private String GOOGLE_API_KEY;

    // =========================================================
    // PUBLIC: 스마트 거리 계산
    // =========================================================
    public DistanceResult getSmartDistance(double originLat, double originLng,
                                           double destLat, double destLng,
                                           String userMode) {

        String normalized = (userMode == null || userMode.isBlank())
                ? "SUBWAY"
                : userMode.toUpperCase();

        log.info("🚇 getSmartDistance 호출: userMode={}, origin=({}, {}), dest=({}, {})",
                normalized, originLat, originLng, destLat, destLng);

        // 1) SUBWAY / BUS → 차량 기준 추정 (+ 도보 판단)
        if (normalized.equals("SUBWAY") || normalized.equals("BUS")) {

            // 먼저 driving으로 베이스값 가져오기
            DistanceResult driving = callDistance(
                    originLat, originLng,
                    destLat, destLng,
                    "driving",
                    null
            );

            if (driving.statusOk) {

                double distanceKm = driving.distanceMeters / 1000.0;

                // 1-1) 아주 가까우면 → 그냥 걷자 (예: 700m 이내)
                if (distanceKm <= 0.7) {
                    int walkingMinutes = estimateWalkingMinutes(distanceKm);

                    log.info("🚶 짧은 거리({}km) → {} 대신 도보 {}분 선택",
                            String.format("%.2f", distanceKm), normalized, walkingMinutes);

                    return new DistanceResult(
                            driving.distanceMeters,
                            walkingMinutes,
                            "WALK",
                            true
                    );
                }

                // 1-2) 지하철/버스 시간 추정 (공식 그대로)
                DistanceResult transit = estimateTransitFromDriving(driving, normalized);

                // 1-3) 도보와 시간 차이가 별로 없으면 도보로 바꾸기
                int walkingMinutes = estimateWalkingMinutes(distanceKm);

                if (distanceKm <= 2.0 &&          // 2km 이하면
                        transit.durationMinutes - walkingMinutes <= 7) { // 시간 차이 7분 이하

                    log.info("🚶 도보 vs {} 시간 차이 {}분 → 도보 선택 ({}분)",
                            normalized,
                            (transit.durationMinutes - walkingMinutes),
                            walkingMinutes);

                    return new DistanceResult(
                            driving.distanceMeters,
                            walkingMinutes,
                            "WALK",
                            true
                    );
                }

                // 그 외에는 추정한 SUBWAY/BUS 유지
                log.info("✅ {} 추정 사용: {}m, {}분 (차량 {}분 기반)",
                        transit.recommendedMode,
                        transit.distanceMeters,
                        transit.durationMinutes,
                        driving.durationMinutes);

                return transit;

            } else {
                log.warn("⚠️ driving 거리 조회 실패 → WALK / 직선거리 fallback");
            }
        }

        // 2) TAXI / CAR / DRIVING → 그냥 driving 사용
        if (normalized.equals("TAXI") || normalized.equals("CAR") || normalized.equals("DRIVING")) {
            DistanceResult r = callDistance(originLat, originLng, destLat, destLng,
                    "driving", null);
            if (r.statusOk) {
                r.recommendedMode = "TAXI";
                log.info("✅ TAXI 사용: {}m, {}분", r.distanceMeters, r.durationMinutes);
                return r;
            } else {
                log.warn("⚠️ TAXI(driving) 거리 조회 실패 → WALK / 직선거리 fallback");
            }
        }

        // 3) WALK
        if (normalized.equals("WALK") || normalized.equals("WALKING")) {
            DistanceResult r = callDistance(originLat, originLng, destLat, destLng,
                    "walking", null);
            if (r.statusOk) {
                r.recommendedMode = "WALK";
                log.info("✅ WALK 사용: {}m, {}분", r.distanceMeters, r.durationMinutes);
                return r;
            } else {
                log.warn("⚠️ WALK 거리 조회 실패 → 직선거리 fallback");
            }
        }

        // 4) 위에서 처리 안 된 모드들(transit 등)은:
        //    1순위 WALK, 2순위 직선거리로 구제
        DistanceResult walking = callDistance(originLat, originLng, destLat, destLng,
                "walking", null);
        if (walking.statusOk) {
            walking.recommendedMode = "WALK";
            log.info("✅ 대체 수단(WALK) 사용: {}m, {}분", walking.distanceMeters, walking.durationMinutes);
            return walking;
        }

        // 5) 최후의 수단: 직선거리
        log.warn("⚠️ 모든 Distance API 실패 → 직선거리 기반 fallback");
        return calculateStraightLineDistance(originLat, originLng, destLat, destLng, normalized);
    }

    // =========================================================
    // 🚶 도보 시간 추정
    // =========================================================
    private int estimateWalkingMinutes(double distanceKm) {
        // 평균 시속 4km 기준
        int minutes = (int) Math.ceil(distanceKm / 4.0 * 60);

        // 너무 가까운 거리라도 최소 3분은 주자
        return Math.max(minutes, 3);
    }

    // =========================================================
    // 🚇 차량 시간 기반으로 SUBWAY / BUS 추정 (공식 유지)
    // =========================================================
    /**
     * 경험적 공식:
     * - SUBWAY: 차량 시간 × 1.3 + 대기 5분 + (거리별 환승 0~10분)
     * - BUS   : 차량 시간 × 1.5 + 대기 7분 + (장거리 환승 10분)
     */
    private DistanceResult estimateTransitFromDriving(DistanceResult driving, String mode) {

        int drivingMinutes = driving.durationMinutes;
        int distanceMeters = driving.distanceMeters;
        double distanceKm = distanceMeters / 1000.0;

        int estimatedMinutes;

        if ("SUBWAY".equalsIgnoreCase(mode)) {
            // 지하철
            estimatedMinutes = (int) (drivingMinutes * 1.3) + 5;   // 기본 배율 + 대기

            // 거리별 환승 시간 가산
            if (distanceKm > 10) {
                estimatedMinutes += 10;        // 장거리: 환승 2회 가정 (5분×2)
            } else if (distanceKm > 5) {
                estimatedMinutes += 5;         // 중거리: 환승 1회 가정
            }
            // 5km 이하는 환승 없음

            log.info("🚇 지하철 추정: 차량 {}분 → 지하철 약 {}분 (거리 {}km)",
                    drivingMinutes, estimatedMinutes, String.format("%.1f", distanceKm));

            return new DistanceResult(
                    distanceMeters,
                    estimatedMinutes,
                    "SUBWAY",
                    true
            );

        } else {
            // 버스
            estimatedMinutes = (int) (drivingMinutes * 1.5) + 7;

            if (distanceKm > 8) {
                estimatedMinutes += 10;        // 장거리 버스 환승/정체 가산
            }

            log.info("🚌 버스 추정: 차량 {}분 → 버스 약 {}분 (거리 {}km)",
                    drivingMinutes, estimatedMinutes, String.format("%.1f", distanceKm));

            return new DistanceResult(
                    distanceMeters,
                    estimatedMinutes,
                    "BUS",
                    true
            );
        }
    }

    // =========================================================
    // Distance Matrix API 호출 (좌표 사용)
    // =========================================================
    private DistanceResult callDistance(double originLat, double originLng,
                                        double destLat, double destLng,
                                        String apiMode,
                                        String transitSubMode) {

        try {
            long departureTime = Instant.now().getEpochSecond() + 3600;

            String originParam = String.format("%.6f,%.6f", originLat, originLng);
            String destParam = String.format("%.6f,%.6f", destLat, destLng);

            String url;

            // 여기서는 transit 거의 안 쓰지만, 혹시 나중 확장용으로 남겨둠
            if ("transit".equalsIgnoreCase(apiMode)) {
                StringBuilder sb = new StringBuilder();
                sb.append("https://maps.googleapis.com/maps/api/distancematrix/json?")
                        .append("origins=").append(originParam)
                        .append("&destinations=").append(destParam)
                        .append("&mode=transit")
                        .append("&departure_time=").append(departureTime)
                        .append("&language=ko")
                        .append("&key=").append(GOOGLE_API_KEY);

                if (transitSubMode != null && !transitSubMode.isBlank()) {
                    sb.append("&transit_mode=").append(transitSubMode);
                }

                url = sb.toString();
            } else {
                url = String.format(
                        "https://maps.googleapis.com/maps/api/distancematrix/json?" +
                                "origins=%s&destinations=%s&mode=%s&language=ko&key=%s",
                        originParam, destParam, apiMode, GOOGLE_API_KEY
                );
            }

            log.info("🌐 Distance API 요청: mode={}, url={}", apiMode, url);

            Request request = new Request.Builder().url(url).build();

            try (Response response = client.newCall(request).execute()) {

                if (!response.isSuccessful()) {
                    log.error("❌ Distance API HTTP 실패: code={}", response.code());
                    return new DistanceResult(999999, 999, apiMode, false);
                }

                ResponseBody body = response.body();
                if (body == null) {
                    log.error("❌ Distance API 응답 body 없음");
                    return new DistanceResult(999999, 999, apiMode, false);
                }

                String bodyStr = body.string();
                log.info("📄 Distance API 응답 일부: {}", bodyStr.substring(0, Math.min(400, bodyStr.length())));

                JsonObject root = JsonParser.parseString(bodyStr).getAsJsonObject();
                String apiStatus = root.get("status").getAsString();

                if (!"OK".equals(apiStatus)) {
                    log.error("❌ Distance API status: {}", apiStatus);
                    return new DistanceResult(999999, 999, apiMode, false);
                }

                JsonArray rows = root.getAsJsonArray("rows");
                if (rows == null || rows.size() == 0) {
                    return new DistanceResult(999999, 999, apiMode, false);
                }

                JsonObject element = rows.get(0)
                        .getAsJsonObject()
                        .getAsJsonArray("elements")
                        .get(0)
                        .getAsJsonObject();

                String elementStatus = element.get("status").getAsString();

                if (!"OK".equals(elementStatus)) {
                    log.error("❌ Element status: {}", elementStatus);
                    return new DistanceResult(999999, 999, apiMode, false);
                }

                int distance = element.getAsJsonObject("distance").get("value").getAsInt();      // m
                int duration = element.getAsJsonObject("duration").get("value").getAsInt() / 60; // 분

                log.info("✅ Distance 성공: {}m, {}분 (mode={})", distance, duration, apiMode);
                return new DistanceResult(distance, duration, apiMode, true);
            }

        } catch (Exception e) {
            log.error("❌ Distance API 예외", e);
            return new DistanceResult(999999, 999, apiMode, false);
        }
    }

    // =========================================================
    // 유틸리티
    // =========================================================
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    private DistanceResult calculateStraightLineDistance(
            double lat1, double lng1, double lat2, double lng2, String userMode) {

        double distanceKm = haversineKm(lat1, lng1, lat2, lng2);
        int distanceMeters = (int) (distanceKm * 1000);

        int durationMinutes;
        String displayMode;

        if ("SUBWAY".equalsIgnoreCase(userMode) || "BUS".equalsIgnoreCase(userMode)) {
            durationMinutes = (int) (distanceKm / 30.0 * 60);  // 평균 30km/h
            displayMode = "SUBWAY";
        } else if ("TAXI".equalsIgnoreCase(userMode) || "CAR".equalsIgnoreCase(userMode) || "DRIVING".equalsIgnoreCase(userMode)) {
            durationMinutes = (int) (distanceKm / 40.0 * 60);  // 평균 40km/h
            displayMode = "TAXI";
        } else {
            durationMinutes = (int) (distanceKm / 4.0 * 60);   // 도보 4km/h
            displayMode = "WALK";
        }

        log.info("📏 직선거리 fallback: {}m, 예상 {}분 (mode={})", distanceMeters, durationMinutes, displayMode);
        return new DistanceResult(distanceMeters, durationMinutes, displayMode, true);
    }

    // =========================================================
    // DTO들
    // =========================================================
    public static class DistanceResult {
        public int distanceMeters;      // m
        public int durationMinutes;     // 분
        public String recommendedMode;  // SUBWAY / BUS / TAXI / WALK
        public boolean statusOk;

        public DistanceResult(int distanceMeters, int durationMinutes,
                              String recommendedMode, boolean statusOk) {
            this.distanceMeters = distanceMeters;
            this.durationMinutes = durationMinutes;
            this.recommendedMode = recommendedMode;
            this.statusOk = statusOk;
        }
    }
}
