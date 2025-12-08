package com.example.WayGo.Service.Itinerary;

import com.example.WayGo.Constant.ErrorCode;
import com.example.WayGo.Dto.Itinerary.*;
import com.example.WayGo.Exception.ItineraryException;
import com.google.gson.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService {

    @Value("${openai.api.key}")
    private String OPENAI_API_KEY;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .build();

    private final GoogleDistanceService distanceService;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4.1-mini";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // =========================================================
    // ✨ 서버는 후보 + 데이터만 제공, 조합/루트/텍스트는 AI가 함
    // =========================================================
    public ItineraryAiResult generateWithCandidates(
            ItineraryGenerationRequestDto req,
            List<WeatherInfoDto> weather,
            List<PlaceDto> places,
            List<FlightRecommendationDto> flights,
            List<HotelRecommendationDto> hotels,
            String userMode
    ) {
        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPromptForB(req, weather, places, flights, hotels, userMode);

            // ✅ totalDays 직접 계산
            LocalDate start = LocalDate.parse(req.getStartDate());
            LocalDate end = LocalDate.parse(req.getEndDate());
            int totalDays = (int) ChronoUnit.DAYS.between(start, end) + 1;

            log.info("🧠 OpenAI 요청 준비 완료. city={}, days={}, places={}, userMode={}",
                    req.getDestinationCity(),
                    totalDays,
                    places != null ? places.size() : 0,
                    userMode);

            String content = callOpenAI(systemPrompt, userPrompt);
            ItineraryAiResult result = parseAiResult(content);

            // 🔥 여기서 JSON 기반으로 중복 장소 제거
            result = ensureNoDuplicatePlaces(result, places);

            log.info("✅ OpenAI 응답 파싱 완료");
            return result;

        } catch (ItineraryException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ OpenAI 일정 생성 중 예외 발생", e);
            throw new ItineraryException(
                    ErrorCode.API_TIMEOUT,
                    "OpenAI 일정 생성 중 오류가 발생했습니다.",
                    e
            );
        }
    }

    // 🔥 같은 장소 중복 방문 후처리
    private ItineraryAiResult ensureNoDuplicatePlaces(
            ItineraryAiResult result,
            List<PlaceDto> placeDtos
    ) {
        String json = result.getJson();
        if (json == null || json.isBlank() || json.equals("{}")) {
            // JSON이 없으면 손댈 수 없음 → 그대로 반환
            return result;
        }

        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("days")) return result;

            // placeDtos를 name → PlaceDto 로 매핑(대체 장소 찾을 때 사용)
            Map<String, PlaceDto> nameToPlace = (placeDtos == null)
                    ? Collections.emptyMap()
                    : placeDtos.stream().collect(Collectors.toMap(
                    PlaceDto::getName,
                    p -> p,
                    (a, b) -> a
            ));

            Set<String> usedNames = new HashSet<>();

            JsonArray days = root.getAsJsonArray("days");
            for (int i = 0; i < days.size(); i++) {
                JsonObject day = days.get(i).getAsJsonObject();
                if (!day.has("activities")) continue;

                JsonArray activities = day.getAsJsonArray("activities");
                for (int j = 0; j < activities.size(); j++) {
                    JsonObject act = activities.get(j).getAsJsonObject();

                    String activityType = act.has("activityType")
                            ? act.get("activityType").getAsString()
                            : "";

                    // PLACE/MEAL만 중복 체크
                    if (!"PLACE".equals(activityType) && !"MEAL".equals(activityType)) {
                        continue;
                    }

                    String name = act.has("name") ? act.get("name").getAsString() : null;
                    if (name == null || name.isBlank()) continue;

                    if (usedNames.contains(name)) {
                        // 🔴 중복 발견 → 다른 후보로 교체하거나 ETC/자유시간으로 변경
                        replaceWithAnotherPlace(act, activityType, usedNames, nameToPlace);
                    } else {
                        usedNames.add(name);
                    }
                }
            }

            String fixedJson = root.toString();
            return ItineraryAiResult.builder()
                    .text(result.getText())
                    .json(fixedJson)
                    .dayTexts(result.getDayTexts()) // 필요하면 dayTexts도 다시 계산 가능
                    .build();

        } catch (Exception e) {
            log.warn("⚠ 중복 장소 후처리 중 오류: {}", e.getMessage());
            return result;
        }
    }

    private void replaceWithAnotherPlace(
            JsonObject act,
            String activityType,
            Set<String> usedNames,
            Map<String, PlaceDto> nameToPlace
    ) {
        // name 필드가 없으면 그냥 자유 시간으로 전환
        if (!act.has("name")) {
            act.addProperty("activityType", "ETC");
            act.addProperty("name", "자유 시간");
            return;
        }

        // String currentName = act.get("name").getAsString(); // 필요하면 사용

        // 지금은 "이름만 안 겹치게" 간단 처리
        for (PlaceDto p : nameToPlace.values()) {
            if (usedNames.contains(p.getName())) continue;

            // MEAL이면 FOOD 포함된 곳만 쓰고 싶으면 여기에 조건 추가 가능:
            // if ("MEAL".equals(activityType) && (p.getStyleTags() == null || !p.getStyleTags().contains("FOOD"))) continue;

            act.addProperty("name", p.getName());
            act.addProperty("address", p.getAddress());
            if (p.getRating() != null) {
                act.addProperty("rating", p.getRating());
            }
            usedNames.add(p.getName());
            return;
        }

        // 대체 후보가 진짜 하나도 없으면 차라리 ETC로 바꿔버리기
        act.addProperty("activityType", "ETC");
        act.addProperty("name", "자유 시간");
    }

    // =========================================================
    // System Prompt – TEXT/JSON + 포맷 & 시간 규칙
    // =========================================================
    private String buildSystemPrompt() {
        return """
        ════════════════════════════════════════════════════════
        🚫🚫🚫 최우선 규칙 - 절대 위반 금지! 🚫🚫🚫
        ════════════════════════════════════════════════════════

        ⚠️ 이 규칙들은 다른 모든 규칙보다 우선합니다!

        규칙 1: placeInputs에 있는 실제 장소만 사용
        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        ❌ 절대 금지:
           - "Shanghai Museum 근처 식당"
           - "Shanghai Church 인근 레스토랑"
           - "주변 맛집", "인근 카페"
           - 이런 형태는 모두 금지!

        ✅ 반드시:
           - placeInputs에 있는 정확한 name만 사용
           - 예: "南翔馒头店", "小杨生煎"

        규칙 2: 같은 장소 2번 이상 사용 절대 금지
        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        - 같은 placeId 재사용 금지
        - 같은 name 재사용 금지
        - 식당도 마찬가지 - 1번만!
        - 총 통틀어 여행중에 같은 장소는 없게하기 

        ❌ 잘못된 예:
        1일차 점심: 上海城隍庙
        2일차 점심: 上海城隍庙  ← 금지!
        
        1일차 14:00 천주교 다이묘마치 주교좌성당 방문
        2일차 11:00 천주교 다이묘마치 주교좌성당 방문 ← 금지!

        ✅ 올바른 예:
        1일차 점심: 식당 A
        2일차 점심: 식당 B (완전히 다른 식당)
        3일차 점심: 식당 C (또 다른 식당)
        
        

        📋 일정 생성 전 체크리스트:
        1. placeInputs에서 사용 가능한 장소 확인
        2. 필요한 장소 수 계산
           - 3일 여행: 점심 3개 + 저녁 3개 = 6개 식당
           - RELAXED: 관광지 2~3개 × 3일 = 6~9개
        3. 각 날짜에 배치하며 사용한 장소 기록
        4. 기록된 장소는 절대 재사용 안 함!

        ════════════════════════════════════════════════════════

        당신은 여행 일정 전문가 AI입니다.
        서버가 미리 계산한 "후보 장소 / 날씨 / 항공 / 숙소 / 거리 정보"만 사용하여
        한국인 사용자를 위한 여행 일정을 생성합니다.

        ❗중요 제약
        - 서버가 제공한 후보만 사용합니다.
          - 장소: placeInputs 배열 안의 id/name/address/lat/lng/styleTags/rating 만 사용
          - 항공: flightInputs 배열만 사용 (새 항공편/시간/가격을 만들지 말 것)
          - 숙소: hotelInputs 배열만 사용
          - 거리/이동시간: distanceLegInputs 안의 durationMinutes, mode, fromName/toName 만 사용
        - 새로운 장소/항공/숙소/이동시간을 지어내지 마세요.
        - 모든 금액은 TEXT에서는 "₩150,000" 처럼 표시하되,
          JSON에서는 쉼표/원/₩ 없이 정수 값만 사용합니다. (예: 150000)

        ========================================================
        🔢 여행 스타일 개수 / 스케줄 밀도 전제
        ========================================================
        - 클라이언트에서 여행 스타일은 **최대 2개**까지 전달됩니다.
          예) ["FOOD", "CULTURE"], ["FOOD", "RELAXATION"], ["NATURE"], ...

        - 스케줄 밀도(scheduleDensity)는 두 가지입니다.
          • RELAXED : 여유 있는 일정
          • PACKED  : 빡빡한 일정

        - 하루당 장소 수 기본 규칙:
          1) PACKED 스케줄
             - 하루에 **정확히 4개의 주요 장소**를 방문합니다.
               (placeInputs 기반의 활동, MEAL/PLACE/CAFE/디저트 등 포함)
             - 스타일이 2개라면 "2 + 2" 비율로 구성합니다.
               예) FOOD + CULTURE
                 → FOOD 스타일 장소 2곳 + CULTURE 스타일 장소 2곳
          2) RELAXED 스케줄
             - 하루에 **2~3개의 주요 장소**를 방문합니다.
             - 스타일이 2개라면
               • 기본: 각 스타일 1곳씩 (총 2곳)
               • 필요시: 한 스타일 2곳 + 다른 스타일 1곳 (총 3곳)

        ========================================================
        ⏰ 시간/동선 계산 규칙 (RELAXED vs PACKED)
        ========================================================
        - 모든 활동의 실제 시간 = 이동시간 + 체류시간
        - 이동시간: distanceLegInputs.durationMinutes 사용
        - 점심: 기본 60분
        - 저녁: 기본 90분
        - 카페/디저트: 45~60분 정도로 가정 (정확 시간은 일정 흐름에 맞게 자연스럽게 조정)

        - RELAXED:
          • 하루 2~3개 주요 활동
          • 충분한 여유 시간(휴식, 이동 여유)을 포함
        - PACKED:
          • 하루 4개 주요 활동
          • 활동 간 이동시간/식사시간을 촘촘히 배치하되, 너무 무리되지 않게 구성

        ========================================================
        ⏰⏰⏰ 시간 계산 규칙 (확장)
        ========================================================
        반드시 아래 규칙을 반영하여 하루 일정 전체를 "타임라인 형식"으로 작성하세요

        1) ⏰ **기본 시간 계산 공식**
        --------------------------------------------------------
        • 활동 소요시간 = 이동시간(durationMinutes) + 체류시간
        • 점심 식사 = "60분 고정"
        • 저녁 식사 = "90분 고정"
        • 카페/디저트 = 45~60분

        2) ⏰ **형식: HH:MM ~ HH:MM 시간표 형태로 반드시 표현**
        --------------------------------------------------------
        예)
        11:00 ~ 12:30 장소1 방문
        12:30 ~ 13:30 점심 식사 (60분)
        13:30 ~ 18:00 장소2 관람
        18:00 ~ 19:30 저녁 식사 (90분)

        3) 🧭 **RELAXED(느슨한 일정) 시간 배치 규칙**
        --------------------------------------------------------
        • 하루 2~3개 주요 장소를 넉넉하게 배치
        • 오전 1곳 → 점심 → 오후 1곳 → 저녁
        • 주요 활동 사이 남는 시간이 1~3시간 발생할 수 있음

        4) 🧭 RELAXED일 때 "남는 시간 활용 팁" 반드시 제공
        --------------------------------------------------------
        활동 후 시간이 남을 경우 다음 중 1개 이상 포함:
        • 주변 카페/디저트 휴식
        • 주변 거리 산책
        • 사진 촬영 / 포토존 추천
        • 기념품 쇼핑 추천
        • 넓은 벤치/야외 공간에서 휴식 팁

        5) 🧭 **PACKED(빡빡한 일정) 시간 배치 규칙**
        --------------------------------------------------------
        예)
        08:00 ~ 10:00 장소1
        10:00 ~ 12:00 장소2
        12:00 ~ 13:00 점심
        13:00 ~ 15:00 장소3
        15:00 ~ 17:00 장소4
        18:30 ~ 20:00 저녁

        6) 🧭 **시간이 너무 촘촘하거나 남는 경우 처리 규칙**
        --------------------------------------------------------
        • 활동 시간이 남으면 여유 시간 팁 제공
        • 시간이 부족하면 다음 중 적용:
          - "핵심 전시/하이라이트 중심으로 관람"
          - "포토존 위주로 빠르게 관람"
          - "대기시간을 줄이는 팁"

        7) 🔥 **모든 날짜는 반드시 '시간표 기반 타임라인'으로 출력**
        --------------------------------------------------------
        - 이동 시작 시각 → 이동 소요시간 → 도착 시각
        - 체류 시작 시각 → 종료 시각 → 다음 일정

        ========================================================
        🎨 여행 스타일 혼합 규칙 (CRITICAL)
        ========================================================
         사용자가 선택한 여행 스타일이 여러 개일 경우, 반드시 모든 스타일을 골고루 섞어서 일정을 구성해야 합니다.

        1) 스타일별 장소 배분 원칙
        --------------------------------------------------------
        - 예: FOOD + CULTURE 스타일인 경우 (스타일 2개)
          • PACKED (4곳/일): FOOD 2곳 + CULTURE 2곳
          • RELAXED (2~3곳/일):
            - 패턴 A: FOOD 1곳 + CULTURE 1곳 (2곳)
            - 패턴 B: FOOD 2곳 + CULTURE 1곳 (3곳)
            - 패턴 C: FOOD 1곳 + CULTURE 2곳 (3곳)

        - 예: FOOD + RELAXATION 스타일인 경우
          • PACKED:
            - 점심(Food) + 디저트/카페(Relax) + 가벼운 산책/전망(Relax) + 저녁(Food)
          • RELAXED:
            - 점심(Food) + 카페/디저트(Relax)
            - 또는 점심(Food) + 저녁(Food) + 카페(Relax)

        - 예: 스타일이 1개인 경우 (예: CULTURE만)
          • PACKED: 하루 4곳, 서로 다른 유형의 문화 장소(사원/신사/박물관/거리 등)를 섞어서 구성
          • RELAXED: 하루 2~3곳, 동선 좋게 배치

        2) 한 가지 스타일로 치우치지 않기
        --------------------------------------------------------
        - ❌ 나쁜 예: FOOD + CULTURE인데 Day 1에 음식점 4곳만 방문
        - ❌ 나쁜 예: FOOD + RELAXATION인데 카페/디저트 4곳만 방문
        - ✅ 좋은 예: Day 1에 음식점 2곳 + 카페/디저트 1곳 + 사찰/명소 1곳 같이 섞어서 구성

        3) FOOD 스타일 + 디저트/카페 조합 (중요)
        --------------------------------------------------------
        - travelStyles에 "FOOD"가 포함될 경우:
          • 하루에 "식사 2회(점심 + 저녁)"가 기본이 되도록 구성합니다.
          • PACKED:
            - 예시: 점심(Food) + 디저트/카페(Relax/ Food) + 관광/쇼핑/문화(다른 스타일) + 저녁(Food)
            - 같은 종류의 식당(예: 케밥집 3곳)은 하루에 2곳을 넘지 않게 제한
          • RELAXED:
            - 예시1: 점심(Food) + 관광/문화(다른 스타일)
            - 예시2: 점심(Food) + 카페/디저트(Relax/Food) + 저녁(Food)

        - 디저트/카페는 다음과 같이 해석:
          • styleTags에 FOOD + RELAXATION 이 같이 있는 장소
          • 또는 이름/주소에 "cafe", "coffee", "dessert", "cake" 등이 포함된 장소

        4) placeInputs의 styleTags 활용
        --------------------------------------------------------
        - 각 장소는 styleTags 필드를 가지고 있습니다.
        - 일정을 짤 때 이 tags를 참고하여 스타일을 골고루 선택하세요.
        - 예: styleTags = ["FOOD", "CULTURE"]인 장소는 두 스타일 모두 만족

        5) 하루 일정 구성 가이드 (요약)
        --------------------------------------------------------
        - RELAXED 스케줄:
          • 하루 2~3개 장소
          • 스타일 2개일 경우: 최소 각 1개 이상 포함
        - PACKED 스케줄:
          • 하루 정확히 4개 장소 (주요 활동 기준)
          • 스타일 2개일 경우: 2 + 2 비율이 기본
        - 하루에 같은 타입(예: 음식점만 4곳, 사찰만 4곳 등)으로 도배하지 않습니다.

        ========================================================
        🚇 이동수단 선택 규칙 (CRITICAL - TAXI 남발 방지)
        ========================================================
        
        ⚠️ 가장 중요: 사용자가 선택한 이동수단(userTransportMode)을 최우선으로 사용하세요!
        
        1) 사용자 선택 이동수단 우선 원칙
        --------------------------------------------------------
        - 서버는 "userTransportMode" 필드로 사용자가 선택한 이동수단을 제공합니다
          • 값 예시: "SUBWAY", "TAXI", "WALK", "BUS" 등

        - distanceLegInputs의 각 구간은 이미 사용자 선택에 맞게 계산된 mode를 포함합니다
          • 예: { "fromName": "A", "toName": "B", "mode": "SUBWAY", "durationMinutes": 25 }

        - ✅ 핵심 원칙: distanceLegInputs에서 제공하는 mode를 그대로 사용하세요!
        - ❌ 절대 금지: mode를 임의로 TAXI로 바꾸지 마세요!

        2) 거리/시간별 이동수단 적용 가이드
        --------------------------------------------------------

        📍 0~10분 거리:
        • userMode가 WALK → 🚶 도보 사용
        • userMode가 SUBWAY → 🚇 지하철 사용 (짧은 거리도 가능)
        • userMode가 TAXI → 🚕 택시 사용
        • ⚠️ 주의: userMode가 SUBWAY인데 5분 거리를 TAXI로 바꾸지 말 것!

        📍 11~30분 거리:
        • userMode가 WALK → 🚶 도보 (가능하면)
        • userMode가 SUBWAY/BUS → 🚇 지하철/버스 (강력 권장)
        • userMode가 TAXI → 🚕 택시
        • ⚠️ 주의: 이 구간에서 TAXI 남발 금지!

        📍 31~60분 거리:
        • userMode가 SUBWAY/BUS → 🚇 지하철/버스 (강력 권장)
        • userMode가 TAXI → 🚕 택시 (허용되지만 하루 2회 이내로 제한)
        • WALK는 비추천 (40분 이상 걷기는 부담)

        📍 61분 이상 거리:
        • 장거리 이동으로 간주
        • userMode가 SUBWAY/BUS → 🚇 지하철/버스
        • userMode가 TAXI → 🚕 택시 (하루 1회만 허용)

        3) 절대 금지 사항
        --------------------------------------------------------
        ❌ 하루 일정에서 모든 이동을 TAXI로만 구성하지 마세요
           예: 5분 이동 TAXI, 10분 이동 TAXI, 15분 이동 TAXI ← 이런 식은 절대 안됨!

        ❌ userMode가 SUBWAY인데 모든 이동을 TAXI로 바꾸지 마세요
           예: distanceLeg에 mode="SUBWAY"인데 TEXT/JSON에서 TAXI로 변경 ← 금지!

        ❌ 짧은 거리(10분 이하)를 TAXI로 이동하는 것을 남발하지 마세요
           예: 5분 거리를 TAXI로 3번 연속 ← 금지!

        ❌ distanceLegInputs의 mode를 무시하고 임의로 변경하지 마세요
           예: leg에 "mode": "SUBWAY"인데 "🚕 택시 이동"으로 표현 ← 금지!

        4) 권장 이동 패턴
        --------------------------------------------------------

        ✅ userMode = SUBWAY일 때:
           하루 구성: WALK 1~2회 + SUBWAY 2~3회 혼합
           예시:
           - 09:00 🚶 도보 (5분)
           - 10:30 🚇 지하철 (20분)
           - 13:00 🚶 도보 (8분)
           - 15:30 🚇 지하철 (25분)

        ✅ userMode = WALK일 때:
           하루 구성: WALK 3~4회 + SUBWAY 1~2회 혼합
           예시:
           - 09:00 🚶 도보 (10분)
           - 11:00 🚶 도보 (15분)
           - 13:30 🚇 지하철 (30분) ← 먼 거리만 대중교통
           - 16:00 🚶 도보 (12분)

        ✅ userMode = TAXI일 때:
           하루 구성: TAXI 2~3회 + WALK 1~2회 혼합
           예시:
           - 09:00 🚕 택시 (15분)
           - 11:30 🚶 도보 (5분) ← 짧은 거리는 도보
           - 14:00 🚕 택시 (20분)
           - 17:30 🚶 도보 (8분)

        5) 날씨에 따른 보정
        --------------------------------------------------------

        ☔ 비/폭우일 때:
        • 15분 이상 도보(WALK)는 피하고 SUBWAY 또는 TAXI 사용
        • ⚠️ 그래도 짧은 거리(5~10분)를 모두 TAXI로 만들지는 말 것
        • 예: 5분 거리는 그냥 걸어도 됨 (우산 활용)

        🌡️ 폭염/한파일 때:
        • 도보 구간은 10분 이내로 제한 권장
        • 10분 이상 거리는 대중교통 우선

        ☀️ 좋은 날씨:
        • userMode에 따라 자유롭게 선택
        • WALK도 부담 없음

        6) TEXT 표현 규칙
        --------------------------------------------------------
        이동은 다음 패턴으로 표현:
        • "09:30 🚇 지하철 이동 (20분, ₩1,200)"
        • "11:10 🚶 도보 (6분)"
        • "17:00 🚕 택시 이동 (25분, ₩7,000)"

        패턴: "HH:MM [아이콘] 이동수단명 (소요시간, 비용)"

        ========================================================
        ✈️ 항공/호텔 & FOOD 스타일 구성 규칙
        ========================================================
        - 항공 후보(flightInputs)의 상세 추천 리스트는
          TEXT에서 **1일차 블록에서만**, **최대 3개**만 보여줍니다.
          
          1일차 일정은 반드시 공항 도착으로 시작합니다.
          - flightInputs에서 도착 시간이 가장 이른 항공편을 선택해 공항 도착 시간을 설정합니다.
          - 도시가 상하이이면 푸동/홍차오 국제공항,
            도쿄이면 하네다/나리타 등 실제 도시에 맞는 공항명을 자동 적용합니다.
          - 공항 도착 → 호텔 이동 → 체크인 → 첫 관광의 순서를 유지하십시오.

        - 호텔(hotelInputs)은:
          • 1일차 저녁에 ACCOMMODATION 활동으로 체크인 1번만 상세히 표현합니다.
          • 2일차 이후에는 "호텔 복귀 및 휴식" 정도로만 TEXT에 한 줄 표현하고,
            JSON에는 새로운 ACCOMMODATION 활동을 만들지 않습니다.
            
                1일차 오전 일정은 반드시 공항 도착으로 시작합니다.
                호텔 추천은 hotelInputs 상위 3개만 TEXT에 표시하고,
                호텔 체크인은 호텔 추천 목록 이후에 오도록 만드세요.    

        - FOOD 스타일 관련:
          • travelStyles에 "FOOD"가 포함되더라도 하루 전체가 음식점만으로 구성되면 안 됩니다.
          • 하루 기준:
            - MEAL(식사/카페/디저트) 활동은 최대 3개까지만 배치
              (점심 1 + 저녁 1 + 디저트/카페 1 정도가 적절)
            - 최소 1개 이상은 PLACE(관광지/액티비티/투어/문화/쇼핑) 타입 활동을 포함
          • 비슷한 종류의 음식점을 같은 날에 3곳 이상 반복하지 않습니다.
        ========================================================
        🏨 숙박비 계산 규칙 (CRITICAL)
        ========================================================      
                  1) 1일차 TEXT의 "추천 호텔 3개" 중에서 가격(price) 값이 가장 높은 호텔을 찾는다.
                     → 이를 "최대 숙박 단가"라고 한다.
                
                  2) 1일차 숙박비 = 최대 숙박 단가
                     2일차 숙박비 = 최대 숙박 단가
                     3일차 숙박비 = 0 (여행 마지막날은 체크인 없음)
                
                  3) TEXT의 "💰 예상 지출" 계산 시 위 숙박비 규칙을 반드시 적용한다.
                  4) JSON의 activities[].mealCost / entranceFee / transportCost 등과 별개로,
                     days[].dailySpent 계산에도 동일 규칙을 적용한다.
                
                  5) 절대 호텔 개별 가격을 일자별로 다르게 적용하지 않는다.
                     → 항상 "추천 호텔 중 가장 비싼 가격 × 숙박일수"를 기준으로 한다.

        ========================================================
        1) ---TEXT---  (날짜별 마크다운 형식)
        ========================================================
                📅 1일차 – 12월 10일 (수) ☀️ 10°C
                💡 오늘의 TIP: 날씨가 좋아요! 가벼운 외투를 챙기세요.
                
                ✈️ 추천 항공편
                - 항공편 A (₩240,000)
                - 항공편 B (₩250,000)
                - 항공편 C (₩270,000)
                
                09:00 ✈️ 공항 도착
                09:30 🚇 지하철 이동 (30분, ₩1,200)
                10:00 ~ 12:00 첫 관광지 방문
                12:00 ~ 13:00 점심 식사
                13:00 ~ ... 오후 일정 계속
                ...
                16:30 호텔 체크인 및 휴식
                
                🏨 추천 호텔
                - 호텔 1 (평점 4.5, 1박 ₩90,000)
                - 호텔 2 (평점 4.4, 1박 ₩85,000)
                - 호텔 3 (평점 4.3, 1박 ₩80,000)
                
                💰 1일차 예상 지출: 교통 ₩6,000 / 식비 ₩80,000 / 입장료 ₩0 / 숙박 ₩90,000 (임의) → 합계 ₩176,000
                ---
                
        ========================================================
        2) ---JSON--- (백엔드 저장용 일정 JSON)
        ========================================================
        - 최상위 스키마:
          {
            "itineraryId": null,
            "title": "도시명 + N일 여행",
            "destinationCity": "도시명",
            "startDate": "YYYY-MM-DD",
            "endDate": "YYYY-MM-DD",
            "totalDays": N,
            "totalBudget": 1500000,
            "totalSpent": 0,
            "days": [ ... ]
          }

        - days[i] 스키마:
          {
            "dayNumber": 1,
            "date": "2025-12-20",
            "weatherCondition": "맑음",
            "temperature": 22,
            "weatherAdvice": "하루 TIP 문장",
            "dailyBudget": 375000,
            "dailySpent": 0,
            "dayText": "이 1일차 전체 TEXT 블록",
            "dayHeader": "📅 1일차 – 12월 20일 (토)",
            "dayContent": "헤더를 제외한 요약 본문",
            "activities": [ ... ]
          }

        - activities[j] 스키마:
          {
            "sequence": 1,
            "activityType": "FLIGHT"|"PLACE"|"MEAL"|"TRANSPORT"|"ACCOMMODATION"|"ETC",
            "name": "장소 또는 활동 이름",
            "address": "주소",
            "startTime": "10:00",
            "endTime": "12:00",
            "durationMinutes": 120,
            "transportType": "SUBWAY"|"BUS"|"WALK"|"TAXI"|null,
            "transportDuration": 25,
            "transportCost": 2000,
            "entranceFee": 0,
            "mealCost": 15000,
            "rating": 4.5,
            "tip": "팁 문장 또는 null"
          }

        ========================================================
        🏷 장소 스타일 표시 규칙 (CRITICAL)
        ========================================================
        - 장소는 원칙적으로 하루 또는 전체 여행에서 "중복 방문"을 하지 않습니다.
                    
        - placeInputs의 각 장소에는 styleTags 배열이 있을 수 있습니다.
          예: ["NATURE", "CITY"], ["FOOD", "RELAXATION"], ["CULTURE"], ...

        - TEXT에서 "장소 방문"을 표현할 때,
          예: "Nicetime Mountain Gallery 방문 (NATURE, CITY, 평점 4.3)"
          처럼 **styleTags 전체를 그대로 나열하지 마세요.**

        - 대신, 아래 규칙에 따라 **대표 스타일 1개만 한국어로 표시**하세요.

          스타일 매핑:
          - NATURE      → "자연 탐방"
          - CITY        → "도시 탐험"
          - FOOD        → "맛집"
          - CULTURE     → "문화·역사"
          - RELAXATION  → "휴식/카페"
          - ACTIVE      → "액티비티"
          - SHOPPING    → "쇼핑"

        - 여러 styleTags가 있을 경우:
          1) 이 장소를 가장 잘 설명하는 스타일 1개를 선택합니다.
          2) 또는, 오늘 일정에서 부족한 스타일을 보완하는 쪽으로 선택해도 좋습니다.
          3) 최종적으로 괄호 안에는 **대표 스타일 1개 + 평점**만 들어가야 합니다.

        - 예시:
          • styleTags = ["NATURE", "CITY"], rating=4.3
            → "Nicetime Mountain Gallery 방문 (자연 탐방, 평점 4.3)"

          • styleTags = ["FOOD"], rating=4.6
            → "이치란 라멘 신주쿠점 방문 (-원, 평점 4.6)"

          • styleTags = ["CITY", "SHOPPING"], rating=4.4
            → "하라주쿠 거리 산책 (도시 탐험, 평점 4.4)"

        ========================================================
        🍽 식사 규칙 (CRITICAL)
        ========================================================
        - 점심/저녁은 반드시 placeInputs 중 styleTags에 FOOD가 포함된 장소에서만 선택합니다.
        - 사용자가 FOOD 스타일을 선택하지 않았더라도 식사는 반드시 FOOD 장소를 하루 2회 포함해야 합니다.
        - 사용자가 "FOOD" 스타일을 선택한 경우:
          - 디저트/카페(RELAXATION + FOOD)를 하루 1~2회 포함해도 좋습니다.
        - "FOOD" 스타일이 아닌 경우:
          - 디저트/카페는 하루 최대 1회 이하로 제한합니다.

        📌 [식당 표현 형식 - 메뉴와 가격 포함]
        --------------------------------------------------------
        - TEXT 영역에서 점심/저녁 식사를 쓸 때는 다음 정보를 함께 표시합니다.
          1) 식당 이름
          2) 대표 메뉴 1~2개
          3) 각 메뉴의 1인 기준 예상 가격 (₩10,000 ~ ₩50,000 사이의 현실적인 금액)

        - 실제 메뉴/가격 정보가 없으므로, "예상" 가능한 전형적인 메뉴 이름과 가격을 만들어서 사용하세요.
          • 예: 라멘집 → "돈코츠 라멘(₩12,000)", "차슈 라멘(₩13,000)"
          • 예: 이자카야 → "모둠 사시미 세트(₩28,000)", "야키토리 세트(₩18,000)"
          • 예: 양식 → "스테이크 디너 코스(₩45,000)", "파스타 세트(₩24,000)"

        - 문장 형식 예시:
          • 점심 식사: Saray Kebab 방문 (맛집, 평점 4.6, 60분)
            - 추천 메뉴: 양고기 케밥 세트(₩18,000), 치킨 케밥 플레이트(₩16,000)

          • 저녁 식사: 뉴욕 그릴 방문 (맛집, 평점 4.5, 90분)
            - 추천 메뉴: 스테이크 디너 코스(₩45,000), 파스타 세트(₩28,000)
            
            식당은 category명을 사용하지 마세요.
            장소 타입은 아래처럼 단순화합니다:
            - 식당: "식사" 또는 "레스토랑"
            - 관광지/교회/역사: 그대로 표시    

        - JSON의 mealCost 값은 위에서 제시한 “1인 예상 메뉴 가격”을 기준으로,
          1인당 평균 비용을 정수로 넣습니다.
          (예: 대표 메뉴가 18,000원, 16,000원이면 mealCost는 18000~20000 정도의 값)
          
                   

        ========================================================
        ✅ 최종 출력 형식
        ========================================================
        1) ---TEXT---
           - 마크다운 형식의 한국어 일정 설명
           - 날짜별로 보기 쉽게 정리 (📅 1일차, 2일차 형식)
           - 이동수단/이동시간은 distanceLegInputs 안의 durationMinutes와 mode를 사용

        2) ---JSON---
           - 위에 정의한 스키마를 따르는 JSON
           - days[].dayText / dayHeader / dayContent / activities 포함
        """;
    }

    // =========================================================
    // User Prompt
    // =========================================================
    private String buildUserPromptForB(
            ItineraryGenerationRequestDto req,
            List<WeatherInfoDto> weather,
            List<PlaceDto> places,
            List<FlightRecommendationDto> flights,
            List<HotelRecommendationDto> hotels,
            String userMode
    ) {
        String weatherSummary = buildWeatherSummary(weather);

        List<Map<String, Object>> placeInputs = buildPlaceInputs(req, places);
        List<Map<String, Object>> distanceLegInputs = buildDistanceLegInputs(places, userMode);
        List<Map<String, Object>> flightInputs = buildFlightInputs(flights);
        List<Map<String, Object>> hotelInputs = buildHotelInputs(hotels);

        String placesJson = GSON.toJson(placeInputs);
        String legsJson = GSON.toJson(distanceLegInputs);
        String flightsJson = GSON.toJson(flightInputs);
        String hotelsJson = GSON.toJson(hotelInputs);

        String stylesStr = (req.getTravelStyles() != null && !req.getTravelStyles().isEmpty())
                ? String.join(", ", req.getTravelStyles())
                : "없음";

        String scheduleDensity = (req.getScheduleDensity() != null)
                ? req.getScheduleDensity().name()
                : "RELAXED";

        // 🔹 이동수단 한글 변환
        String userModeKo = convertModeToKorean(userMode);

        // 🔹 여행 스타일 분석 가이드
        String styleGuide = buildStyleGuide(req.getTravelStyles());

        return """
                - 같은 name의 장소가 TEXT에 두 번 이상 등장하면, 그 응답은 '잘못된 일정'입니다.
                - 일정을 생성할 때, 이미 사용한 장소 이름 목록을 머릿속에 저장하고,
                  새로운 활동을 추가할 때마다 '이름이 중복되는지'를 반드시 먼저 확인하세요.
                - 만약 실수로 중복 장소를 사용했다면, 마지막에 반드시 스스로 찾아서 다른 장소로 교체하세요.

            [사용자 요청 정보]
            - 도시: %s
            - 여행 날짜: %s ~ %s
            - 총 인원: %d명
            - 여행 스타일: %s ⚠️ 최대 2개, 두 스타일을 하루 일정에 골고루 섞어야 합니다!
            - 스케줄 밀도: %s
            - 총 예산: ₩%,d

            [🚇 사용자 선택 이동수단 - CRITICAL]
            사용자가 선택한 이동수단: %s (%s)

            ⚠️ 중요 규칙:
            1. distanceLegInputs의 mode는 이미 사용자 선택(%s)에 맞게 계산되어 있습니다
            2. 제공된 mode를 그대로 사용하세요 - 임의로 변경 금지!
            3. 특히 짧은 거리(10분 이하)를 모두 TAXI로 만들지 마세요
            4. 하루 일정에서 이동수단을 다양하게 섞으세요 (WALK, SUBWAY, TAXI 혼합)

            [🎨 여행 스타일 가이드]
            %s

            [날씨 요약]
            %s

            [항공 후보]
            %s

            [호텔 후보]
            %s

            [장소 후보 - placeInputs]
            %s

            [장소간 거리 정보 - distanceLegInputs]
            %s

            위 정보를 활용해 ---TEXT--- 와 ---JSON--- 두 영역을 모두 생성하세요.
            """.formatted(
                req.getDestinationCity(),
                req.getStartDate(),
                req.getEndDate(),
                req.getTravelers() != null ? req.getTravelers() : 1,
                stylesStr,
                scheduleDensity,
                req.getBudget() != null ? req.getBudget() : 0,
                userMode.toUpperCase(),
                userModeKo,
                userMode.toUpperCase(),
                styleGuide,
                weatherSummary,
                flightsJson,
                hotelsJson,
                placesJson,
                legsJson
        );
    }

    /**
     * 🔹 이동수단 한글 변환
     */
    private String convertModeToKorean(String mode) {
        return switch (mode.toUpperCase()) {
            case "SUBWAY", "TRANSIT" -> "지하철/대중교통";
            case "BUS" -> "버스";
            case "WALK", "WALKING" -> "도보";
            case "TAXI", "DRIVING" -> "택시/자동차";
            default -> mode;
        };
    }

    /**
     * 🔹 여행 스타일 가이드 생성
     */
    private String buildStyleGuide(List<String> travelStyles) {
        if (travelStyles == null || travelStyles.isEmpty()) {
            return "기본 관광 일정으로 구성하세요.";
        }

        StringBuilder guide = new StringBuilder();
        guide.append("선택된 스타일: ").append(String.join(", ", travelStyles)).append("\n");
        guide.append("⚠️ 최대 2개의 스타일만 사용되며, 모든 스타일을 골고루 섞어야 합니다.\n\n");

        int styleCount = travelStyles.size();

        if (styleCount == 1) {
            guide.append("- 하나의 스타일이지만 같은 유형을 반복하지 말고 다양한 장소를 방문하세요.\n");
            guide.append("- 예: FOOD 스타일이어도 일식, 양식, 카페, 디저트 등 다양하게 구성\n");
        } else if (styleCount == 2) {
            guide.append("- PACKED(빡빡한) 일정:\n");
            guide.append("  • 하루에 방문 장소 4곳 → 스타일1 2곳 + 스타일2 2곳\n");
            guide.append("- RELAXED(느슨한) 일정:\n");
            guide.append("  • 하루에 방문 장소 2~3곳 → 기본 1+1, 필요시 한 쪽에서 1곳 더 추가 (2+1)\n\n");
        } else {
            guide.append("- (예외 케이스) 3개 이상이 들어올 경우에도 매일 모든 스타일이 최소 1개씩 포함되도록 하세요.\n");
        }
        guide.append("\n━━━━━━━━━━━━━━━━━━━━━━━━\n");
        guide.append("스타일별 장소 배치 가이드:\n");
        guide.append("━━━━━━━━━━━━━━━━━━━━━━━━\n");

        for (String style : travelStyles) {
            switch (style.toUpperCase()) {
                case "FOOD":
                    guide.append("🍜 FOOD:\n");
                    guide.append("   - 포함: 식당, 카페, 전통 음식점, 맛집, 디저트\n");
                    guide.append("   - PACKED: 점심/저녁 + 디저트/카페 조합, 하루 2~3개의 MEAL/RELAX 계열 활동\n");
                    guide.append("   - 비슷한 종류 반복 금지 (예: 케밥집 3곳 X)\n");
                    break;
                case "CULTURE":
                    guide.append("🏛 CULTURE:\n");
                    guide.append("   - 포함: 박물관, 미술관, 사원, 교회, 역사 유적지\n");
                    guide.append("   - FOOD와 함께 사용 시, 매일 최소 1곳 이상 포함\n");
                    break;
                case "ACTIVE":
                    guide.append("🎢 ACTIVE:\n");
                    guide.append("   - 포함: 액티비티, 체험, 투어, 테마파크\n");
                    guide.append("   - 시간이 오래 걸리는 활동이므로 하루 1~2곳\n");
                    break;
                case "NATURE":
                    guide.append("🌳 NATURE:\n");
                    guide.append("   - 포함: 공원, 정원, 해변, 산책로, 자연 명소\n");
                    guide.append("   - 날씨 좋은 날 우선 배치\n");
                    break;
                case "RELAXATION":
                    guide.append("☕ RELAXATION:\n");
                    guide.append("   - 포함: 카페, 스파, 전망대, 여유로운 산책\n");
                    guide.append("   - FOOD와 함께 쓰면 주로 디저트/카페 용도로 사용\n");
                    break;
                case "SHOPPING":
                    guide.append("🛍 SHOPPING:\n");
                    guide.append("   - 포함: 쇼핑몰, 시장, 쇼핑 거리\n");
                    guide.append("   - 보통 오후/저녁에 배치\n");
                    break;
            }
        }

        guide.append("\n━━━━━━━━━━━━━━━━━━━━━━━━\n");
        guide.append("스타일 혼합 예시:\n");
        guide.append("━━━━━━━━━━━━━━━━━━━━━━━━\n");

        if (travelStyles.contains("FOOD") && travelStyles.contains("CULTURE")) {
            guide.append("✅ FOOD + CULTURE 좋은 예 (PACKED):\n");
            guide.append("   09:00 - 사원 방문 (CULTURE)\n");
            guide.append("   12:00 - 전통 식당 점심 (FOOD)\n");
            guide.append("   14:30 - 박물관 (CULTURE)\n");
            guide.append("   18:00 - 맛집 저녁 (FOOD)\n\n");
        }

        if (travelStyles.contains("FOOD") && travelStyles.contains("RELAXATION")) {
            guide.append("✅ FOOD + RELAXATION 좋은 예 (PACKED):\n");
            guide.append("   12:00 - 점심 식사 (FOOD)\n");
            guide.append("   14:00 - 카페/디저트 (RELAXATION)\n");
            guide.append("   17:30 - 저녁 식사 (FOOD)\n");
            guide.append("   19:30 - 야간 카페/디저트 (RELAXATION)\n");
        }

        return guide.toString();
    }

    // =========================================================
    // OpenAI HTTP 호출
    // =========================================================
    private String callOpenAI(String systemPrompt, String userPrompt) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);

        JsonArray messages = new JsonArray();

        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", systemPrompt);
        messages.add(sys);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userPrompt);
        messages.add(user);

        body.add("messages", messages);
        body.addProperty("temperature", 0.5);

        Request request = new Request.Builder()
                .url(OPENAI_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + OPENAI_API_KEY)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .post(RequestBody.create(
                        body.toString(),
                        MediaType.parse("application/json")
                ))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("❌ OpenAI HTTP 에러: code={}, msg={}",
                        response.code(), response.message());
                throw new ItineraryException(
                        ErrorCode.API_TIMEOUT,
                        "OpenAI 호출 실패: " + response.message()
                );
            }

            String responseBody = response.body().string();
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");

            if (choices == null || choices.size() == 0) {
                throw new ItineraryException(
                        ErrorCode.TRANSLATION_FAILED,
                        "OpenAI 응답에 choices가 없습니다."
                );
            }

            return choices.get(0)
                    .getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content")
                    .getAsString();
        }
    }

    // =========================================================
    // TEXT/JSON 분리 + dayText 추출
    // =========================================================
    private ItineraryAiResult parseAiResult(String content) {
        String textPart = "";
        String jsonPart = "";

        String[] split = content.split("---JSON---");
        if (split.length == 2) {
            String beforeJson = split[0];
            jsonPart = split[1].trim();

            String[] split2 = beforeJson.split("---TEXT---");
            if (split2.length == 2) {
                textPart = split2[1].trim();
            } else {
                textPart = beforeJson.trim();
            }
        } else {
            textPart = content.trim();
            jsonPart = "{}";
        }

        Map<Integer, String> dayTexts = new LinkedHashMap<>();

        try {
            JsonObject jsonRoot = JsonParser.parseString(jsonPart).getAsJsonObject();

            if (jsonRoot.has("days")) {
                JsonArray days = jsonRoot.getAsJsonArray("days");

                for (int i = 0; i < days.size(); i++) {
                    JsonObject d = days.get(i).getAsJsonObject();
                    int dayNumber = d.get("dayNumber").getAsInt();

                    String dayText = d.has("dayText") && !d.get("dayText").isJsonNull()
                            ? d.get("dayText").getAsString()
                            : "";

                    dayTexts.put(dayNumber, dayText);
                }
            }

        } catch (Exception e) {
            log.warn("⚠ JSON 파싱 중 오류: {}", e.getMessage());
        }

        return ItineraryAiResult.builder()
                .text(textPart)
                .json(jsonPart)
                .dayTexts(dayTexts)
                .build();
    }

    // ==============================
    // Weather / Place / Leg builders
    // ==============================
    private String buildWeatherSummary(List<WeatherInfoDto> weather) {
        if (weather == null || weather.isEmpty()) return "날씨 정보 없음";

        return weather.stream()
                .map(w -> "%s: %s, %d°C, TIP: %s".formatted(
                        w.getDate() != null ? w.getDate().toString() : "",
                        w.getConditionKr(),
                        w.getTemperature(),
                        w.getAdvice() != null ? w.getAdvice() : ""
                ))
                .collect(Collectors.joining("\n"));
    }

    private List<Map<String, Object>> buildPlaceInputs(
            ItineraryGenerationRequestDto req,
            List<PlaceDto> places
    ) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (places == null) return result;

        int idx = 1;
        for (PlaceDto p : places) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", "P" + (idx++));
            m.put("placeId", p.getPlaceId());
            m.put("name", p.getName());
            m.put("address", p.getAddress());
            m.put("lat", p.getLat());
            m.put("lng", p.getLng());
            m.put("rating", p.getRating());
            m.put("styleTags", p.getStyleTags());
            result.add(m);
        }
        return result;
    }

    private List<Map<String, Object>> buildDistanceLegInputs(
            List<PlaceDto> places,
            String userMode
    ) {
        List<Map<String, Object>> legs = new ArrayList<>();
        if (places == null || places.size() < 2) return legs;

        int maxPlaces = Math.min(places.size(), 12);
        List<PlaceDto> subset = places.subList(0, maxPlaces);

        for (int i = 0; i < subset.size(); i++) {
            for (int j = i + 1; j < subset.size(); j++) {
                PlaceDto a = subset.get(i);
                PlaceDto b = subset.get(j);

                GoogleDistanceService.DistanceResult dist =
                        distanceService.getSmartDistance(
                                a.getLat(), a.getLng(),
                                b.getLat(), b.getLng(),
                                userMode
                        );

                if (!dist.statusOk) continue;

                String mode = dist.recommendedMode != null
                        ? dist.recommendedMode
                        : userMode;

                String modeKo = switch (mode.toUpperCase()) {
                    case "SUBWAY", "TRANSIT" -> "지하철/대중교통";
                    case "WALK", "WALKING" -> "도보";
                    case "DRIVING", "TAXI", "CAR" -> "자동차/택시";
                    default -> "이동";
                };

                Map<String, Object> leg1 = new LinkedHashMap<>();
                leg1.put("fromId", a.getPlaceId());
                leg1.put("toId", b.getPlaceId());
                leg1.put("fromName", a.getName());
                leg1.put("toName", b.getName());
                leg1.put("mode", mode);
                leg1.put("modeLabelKo", modeKo);
                leg1.put("distanceMeters", dist.distanceMeters);
                leg1.put("durationMinutes", dist.durationMinutes);
                legs.add(leg1);

                Map<String, Object> leg2 = new LinkedHashMap<>();
                leg2.put("fromId", b.getPlaceId());
                leg2.put("toId", a.getPlaceId());
                leg2.put("fromName", b.getName());
                leg2.put("toName", a.getName());
                leg2.put("mode", mode);
                leg2.put("modeLabelKo", modeKo);
                leg2.put("distanceMeters", dist.distanceMeters);
                leg2.put("durationMinutes", dist.durationMinutes);
                legs.add(leg2);
            }
        }

        return legs;
    }

    private List<Map<String, Object>> buildFlightInputs(List<FlightRecommendationDto> flights) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (flights == null) return result;

        for (FlightRecommendationDto f : flights) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("airline", f.getAirline());
            m.put("flightNumber", f.getFlightNumber());
            m.put("departureTime", f.getDepartureTime());
            m.put("arrivalTime", f.getArrivalTime());
            m.put("durationMinutes", f.getDurationMinutes());
            m.put("price", f.getPrice());
            m.put("departureAirport", f.getDepartureAirport());
            m.put("arrivalAirport", f.getArrivalAirport());
            result.add(m);
        }

        return result;
    }

    private List<Map<String, Object>> buildHotelInputs(List<HotelRecommendationDto> hotels) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (hotels == null) return result;

        for (HotelRecommendationDto h : hotels) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", h.getName());
            m.put("address", h.getAddress());
            m.put("lat", h.getLat());
            m.put("lng", h.getLng());
            m.put("rating", h.getRating());
            m.put("pricePerNight", h.getPricePerNight());
            result.add(m);
        }

        return result;
    }
}
