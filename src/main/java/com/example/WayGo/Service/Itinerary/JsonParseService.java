package com.example.WayGo.Service.Itinerary;

import com.example.WayGo.Entity.Itinerary.Itinerary;
import com.example.WayGo.Entity.Itinerary.ItineraryActivity;
import com.example.WayGo.Entity.Itinerary.ItineraryDay;
import com.example.WayGo.Entity.enums.ActivityType;
import com.example.WayGo.Entity.enums.TransportType;
import com.example.WayGo.Repository.Itinerary.ItineraryActivityRepository;
import com.example.WayGo.Repository.Itinerary.ItineraryDayRepository;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
public class JsonParseService {

    private final ItineraryDayRepository dayRepository;
    private final ItineraryActivityRepository activityRepository;

    // ============================================================
    // JSON → DB 저장
    // ============================================================
    @Transactional
    public void parseAndSave(
            Itinerary itinerary,
            String json,
            Map<Integer, String> dayTexts,          // TEXT 일정 (Day 1, 2, 3…)
            Map<Integer, Integer> dailyBudgetMap    // 하루 예산
    ) {
        try {
            log.info("🔄 JSON 파싱 시작 - JSON 길이: {} bytes", json != null ? json.length() : 0);

            // JSON 정제
            String cleanedJson = cleanJsonString(json);
            log.debug("✅ JSON 정제 완료 - 정제 후 길이: {} bytes", cleanedJson.length());

            // Lenient 모드로 파싱
            JsonReader reader = new JsonReader(new StringReader(cleanedJson));
            reader.setLenient(true);  // 🔥 핵심: Lenient 모드 활성화

            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray days = root.getAsJsonArray("days");

            if (days == null || days.size() == 0) {
                log.warn("⚠️ days 배열이 비어있습니다");
                return;
            }

            log.info("📅 총 {} 일의 일정 파싱 시작", days.size());

            for (int i = 0; i < days.size(); i++) {
                JsonObject dayObj = days.get(i).getAsJsonObject();

                int dayNumber = dayObj.get("dayNumber").getAsInt();
                String date = dayObj.get("date").getAsString();

                log.debug("📆 {}일차 처리 중... (날짜: {})", dayNumber, date);

                // ✨ dayText를 헤더와 내용으로 분리
                String fullDayText = dayTexts.get(dayNumber);
                SplitDayText split = splitDayText(fullDayText);

                ItineraryDay day = ItineraryDay.builder()
                        .itinerary(itinerary)
                        .dayNumber(dayNumber)
                        .date(LocalDate.parse(date))
                        .weatherCondition(getString(dayObj, "weatherCondition"))
                        .temperature(getInt(dayObj, "temperature"))
                        .weatherAdvice(getString(dayObj, "weatherAdvice"))
                        .dailyBudget(dailyBudgetMap.get(dayNumber))
                        .dailySpent(0)
                        .dayText(fullDayText)           // 전체 텍스트 (기존 유지)
                        .dayHeader(split.header)        // ✨ 헤더만
                        .dayContent(split.content)      // ✨ 내용만
                        .build();

                dayRepository.save(day);

                JsonArray acts = dayObj.getAsJsonArray("activities");
                if (acts != null) {
                    saveActivities(day, acts);
                    log.debug("✅ {}일차 활동 {} 개 저장 완료", dayNumber, acts.size());
                }
            }

            log.info("✅ 전체 일정 파싱 및 저장 완료");

        } catch (Exception e) {
            log.error("❌ JSON 파싱 중 오류 발생", e);
            log.debug("문제가 된 JSON:\n{}", json);
            throw new RuntimeException("JSON 파싱 실패: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // 🔥 JSON 문자열 정제 (마크다운 코드 블록 제거)
    // ============================================================
    private String cleanJsonString(String rawJson) {
        if (rawJson == null || rawJson.isEmpty()) {
            throw new IllegalArgumentException("JSON이 비어있습니다");
        }

        // 마크다운 코드 블록 제거
        String cleaned = rawJson
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        // JSON 시작 찾기 ({ 또는 [)
        int jsonStart = -1;
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (c == '{' || c == '[') {
                jsonStart = i;
                break;
            }
        }

        if (jsonStart == -1) {
            log.error("❌ JSON 시작 문자를 찾을 수 없습니다");
            log.debug("원본 JSON 앞부분: {}",
                    cleaned.substring(0, Math.min(100, cleaned.length())));
            throw new IllegalArgumentException("유효한 JSON 시작을 찾을 수 없습니다");
        }

        // JSON 끝 찾기 (} 또는 ])
        int jsonEnd = -1;
        for (int i = cleaned.length() - 1; i >= 0; i--) {
            char c = cleaned.charAt(i);
            if (c == '}' || c == ']') {
                jsonEnd = i + 1;
                break;
            }
        }

        if (jsonEnd == -1) {
            log.error("❌ JSON 종료 문자를 찾을 수 없습니다");
            throw new IllegalArgumentException("유효한 JSON 종료를 찾을 수 없습니다");
        }

        cleaned = cleaned.substring(jsonStart, jsonEnd);

        // 중괄호 균형 체크
        long openBraces = cleaned.chars().filter(ch -> ch == '{').count();
        long closeBraces = cleaned.chars().filter(ch -> ch == '}').count();

        if (openBraces != closeBraces) {
            log.warn("⚠️ JSON 중괄호 불균형: {{ = {}, }} = {}", openBraces, closeBraces);
        }

        return cleaned;
    }

    // ============================================================
    // ✨ dayText를 헤더와 내용으로 분리
    // ============================================================
    private SplitDayText splitDayText(String dayText) {
        if (dayText == null || dayText.isEmpty()) {
            return new SplitDayText("", "");
        }

        // 첫 번째 줄(헤더)과 나머지(내용) 분리
        String[] lines = dayText.split("\n", 2);

        String header = lines[0].trim();  // "📅 1일차 – 12월 15일 (월)"
        String content = lines.length > 1 ? lines[1].trim() : "";

        log.debug("📋 Day 분리 완료 - 헤더: {}, 내용 길이: {}", header, content.length());

        return new SplitDayText(header, content);
    }

    // ============================================================
    // Activity 저장
    // ============================================================
    private void saveActivities(ItineraryDay day, JsonArray acts) {
        int sequence = 1;

        for (int i = 0; i < acts.size(); i++) {
            JsonObject a = acts.get(i).getAsJsonObject();

            ActivityType type = classifyActivity(a);

            String name = getString(a, "name");
            String startTime = getString(a, "startTime");
            String endTime   = getString(a, "endTime");

            LocalTime parsedStartTime = parseTimeSafe(startTime);
            LocalTime parsedEndTime   = parseTimeSafe(endTime);

            ItineraryActivity act = ItineraryActivity.builder()
                    .itineraryDay(day)
                    .sequence(sequence++)
                    .activityType(type)
                    .name(name)
                    .startTime(parsedStartTime)
                    .endTime(parsedEndTime)
                    .durationMinutes(getInt(a, "durationMinutes"))
                    .address(getString(a, "address"))
                    .rating(getDouble(a, "rating"))
                    .lat(getDouble(a, "lat"))
                    .lng(getDouble(a, "lng"))
                    .mealCost(parseCost(a, "mealCost"))
                    .entranceFee(parseCost(a, "entranceFee"))
                    .transportType(parseTransportType(getString(a, "transportType")))
                    .transportDuration(getInt(a, "transportDuration"))
                    .transportCost(parseCost(a, "transportCost"))
                    .tip(getString(a, "tip"))
                    .build();

            activityRepository.save(act);
        }
    }

    private LocalTime parseTimeSafe(String time) {
        if (time == null || time.isBlank()) return null;
        try {
            return LocalTime.parse(time);
        } catch (Exception e) {
            log.warn("⚠️ time 파싱 실패: {}", time);
            return null;
        }
    }

    // ============================================================
    // TransportType 파싱
    // ============================================================
    private TransportType parseTransportType(String raw) {
        if (raw == null) return null;

        try {
            return TransportType.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            log.debug("⚠️ TransportType 파싱 실패: {} -> WALK로 기본값 설정", raw);
            return TransportType.WALK; // 기본값
        }
    }

    // ============================================================
    // ActivityType 자동 분류
    // ============================================================
    private ActivityType classifyActivity(JsonObject a) {
        String name = getString(a, "name");
        if (name == null) return ActivityType.ETC;

        String lower = name.toLowerCase();

        if (lower.contains("항공") || lower.contains("비행기") || lower.contains("flight")) {
            return ActivityType.FLIGHT;
        }

        if (lower.contains("호텔") || lower.contains("숙소")) {
            return ActivityType.ACCOMMODATION;
        }

        if (lower.contains("점심") || lower.contains("저녁")
                || lower.contains("식사") || lower.contains("카페")) {
            return ActivityType.MEAL;
        }

        if (lower.contains("도보") || lower.contains("이동")
                || lower.contains("지하철") || lower.contains("버스") || lower.contains("택시")) {
            return ActivityType.TRANSPORT;
        }

        if (lower.contains("박물관") || lower.contains("미술관")
                || lower.contains("성") || lower.contains("랜드마크")) {
            return ActivityType.PLACE;
        }

        return ActivityType.PLACE;
    }

    // ============================================================
    // 문자열 비용 → 숫자 변환
    // ============================================================
    private Integer parseCost(JsonObject a, String key) {
        String raw = getString(a, key);
        if (raw == null) return null;
        try {
            return Integer.parseInt(raw.replaceAll("[^0-9]", ""));
        } catch (Exception ex) {
            log.warn("⚠️ 비용 파싱 실패: {} = {}", key, raw);
            return null;
        }
    }

    // ============================================================
    // 안전 파서 (null-safe)
    // ============================================================
    private String getString(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsString();
    }

    private Integer getInt(JsonObject obj, String key) {
        try {
            return obj.has(key) && !obj.get(key).isJsonNull()
                    ? obj.get(key).getAsInt()
                    : null;
        } catch (Exception e) {
            log.warn("⚠️ Integer 파싱 실패: {} -> null", key);
            return null;
        }
    }

    private Double getDouble(JsonObject obj, String key) {
        try {
            return obj.has(key) && !obj.get(key).isJsonNull()
                    ? obj.get(key).getAsDouble()
                    : null;
        } catch (Exception e) {
            log.warn("⚠️ Double 파싱 실패: {} -> null", key);
            return null;
        }
    }

    // ============================================================
    // ✨ 내부 클래스: 분리된 dayText
    // ============================================================
    @Data
    @AllArgsConstructor
    private static class SplitDayText {
        private String header;   // 헤더 (첫 줄)
        private String content;  // 내용 (나머지)
    }
}