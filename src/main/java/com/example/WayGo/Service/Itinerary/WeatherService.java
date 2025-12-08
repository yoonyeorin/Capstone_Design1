package com.example.WayGo.Service.Itinerary;

import com.example.WayGo.Dto.Itinerary.WeatherInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * WeatherAPI.com 기반 — 위도/경도 기반 날씨 조회 (가장 안정적인 방식)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final OkHttpClient client = new OkHttpClient();

    @Value("${weatherapi.key}")
    private String WEATHER_API_KEY;

    /**
     * start ~ end 날짜 범위의 날씨를 반환 (위도/경도 기반)
     */
    public List<WeatherInfoDto> getWeather(
            String startDateStr,
            String endDateStr,
            Double lat,
            Double lng
    ) {
        LocalDate start = LocalDate.parse(startDateStr);
        LocalDate end = LocalDate.parse(endDateStr);

        // --- WeatherAPI 요청 URL (lat,lng)
        String query = lat + "," + lng;

        String url = String.format(
                "https://api.weatherapi.com/v1/forecast.json?key=%s&q=%s&days=14&aqi=no&alerts=no",
                WEATHER_API_KEY,
                query
        );

        log.info("🌤 WeatherAPI 요청 URL = {}", url);

        try {
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();

            log.info("🌤 WeatherAPI 응답 코드 = {}", response.code());

            if (!response.isSuccessful() || response.body() == null) {
                log.warn("🌤 WeatherAPI 실패 → 기본 날씨 적용");
                return defaultWeather(start, end);
            }

            String json = response.body().string();
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            JsonObject forecast = root.getAsJsonObject("forecast");
            if (forecast == null || !forecast.has("forecastday")) {
                log.warn("🌤 forecast 없음 → 기본 날씨 적용");
                return defaultWeather(start, end);
            }

            JsonArray forecastDays = forecast.getAsJsonArray("forecastday");
            log.info("🌤 forecastday 개수 = {}", forecastDays.size());

            List<WeatherInfoDto> result = new ArrayList<>();
            LocalDate cursor = start;

            while (!cursor.isAfter(end)) {
                WeatherInfoDto dto = extractDay(cursor, forecastDays);
                result.add(dto);
                cursor = cursor.plusDays(1);
            }

            return result;

        } catch (Exception e) {
            log.error("🌤 WeatherAPI 예외 → 기본 날씨로 대체", e);
            return defaultWeather(start, end);
        }
    }

    private WeatherInfoDto extractDay(LocalDate date, JsonArray forecastDays) {

        for (int i = 0; i < forecastDays.size(); i++) {
            JsonObject obj = forecastDays.get(i).getAsJsonObject();

            if (!obj.get("date").getAsString().equals(date.toString())) continue;

            JsonObject day = obj.getAsJsonObject("day");
            JsonObject cond = day.getAsJsonObject("condition");

            int avg = (int) Math.round(day.get("avgtemp_c").getAsDouble());
            int min = (int) Math.round(day.get("mintemp_c").getAsDouble());
            int max = (int) Math.round(day.get("maxtemp_c").getAsDouble());

            String condition = cond.get("text").getAsString();
            boolean rain = condition.toLowerCase().contains("rain");

            return WeatherInfoDto.builder()
                    .date(date.toString())
                    .condition(condition)
                    .conditionKr(toKr(condition))
                    .temperature(avg)
                    .tempMin(min)
                    .tempMax(max)
                    .isRainy(rain)
                    .advice(makeAdvice(avg, rain))
                    .build();
        }

        // 예보 없으면 기본값
        return WeatherInfoDto.builder()
                .date(date.toString())
                .condition("Unknown")
                .conditionKr("정보 없음")
                .temperature(22)
                .tempMin(18)
                .tempMax(26)
                .isRainy(false)
                .advice("기본 날씨 적용")
                .build();
    }

    // === Helper ===
    private String toKr(String c) {
        c = c.toLowerCase();
        if (c.contains("sun")) return "맑음";
        if (c.contains("cloud")) return "흐림";
        if (c.contains("rain")) return "비";
        if (c.contains("snow")) return "눈";
        return "기타";
    }

    private String makeAdvice(int temp, boolean rain) {
        if (rain) return "비가 와요. 우산 챙기세요!";
        if (temp >= 28) return "더워요. 물과 모자를 준비하세요!";
        if (temp <= 5) return "추워요. 두꺼운 옷을 준비하세요!";
        return "날씨가 좋아요!";
    }

    private List<WeatherInfoDto> defaultWeather(LocalDate start, LocalDate end) {
        List<WeatherInfoDto> list = new ArrayList<>();
        LocalDate cursor = start;

        while (!cursor.isAfter(end)) {
            list.add(
                    WeatherInfoDto.builder()
                            .date(cursor.toString())
                            .condition("Clear")
                            .conditionKr("맑음")
                            .temperature(22)
                            .tempMin(18)
                            .tempMax(26)
                            .isRainy(false)
                            .advice("기본 날씨 적용")
                            .build()
            );
            cursor = cursor.plusDays(1);
        }
        return list;
    }
}
