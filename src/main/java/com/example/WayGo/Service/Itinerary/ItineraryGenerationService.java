package com.example.WayGo.Service.Itinerary;

import com.example.WayGo.Constant.ErrorCode;
import com.example.WayGo.Dto.Itinerary.*;
import com.example.WayGo.Entity.Itinerary.Itinerary;
import com.example.WayGo.Entity.UserEntity;
import com.example.WayGo.Entity.enums.ItineraryStatus;
import com.example.WayGo.Entity.enums.ScheduleDensity;
import com.example.WayGo.Exception.ItineraryException;
import com.example.WayGo.Repository.Itinerary.ItineraryRepository;
import com.example.WayGo.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItineraryGenerationService {

    private final ItineraryRepository itineraryRepository;
    private final UserRepository userRepository;

    private final WeatherService weatherService;
    private final GooglePlacesService googlePlacesService;
    private final GoogleDistanceService distanceService;
    private final OpenAIService openAIService;
    private final JsonParseService jsonParseService;
    private final FlightRecommendationService flightRecommendationService;
    private final HotelRecommendationService hotelRecommendationService;

    // ========================================================
    // 1) 일자 겹침 체크
    // ========================================================
    public OverlapCheckResponseDto checkDateOverlap(
            Long userId, String start, String end
    ) {
        LocalDate newStart = LocalDate.parse(start);
        LocalDate newEnd = LocalDate.parse(end);

        List<Itinerary> list = itineraryRepository
                .findByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        userId, newEnd, newStart);

        if (list.isEmpty()) {
            return OverlapCheckResponseDto.builder()
                    .overlap(false)
                    .build();
        }

        Itinerary ex = list.get(0);

        return OverlapCheckResponseDto.builder()
                .overlap(true)
                .existingItineraryId(ex.getId())
                .existingTitle(ex.getTitle())
                .existingStart(ex.getStartDate().toString())
                .existingEnd(ex.getEndDate().toString())
                .build();
    }

    // ========================================================
    // 2) 일정 확정 (GENERATED → ACTIVE)
    //    - 겹치는 다른 일정은 삭제
    // ========================================================
    @Transactional
    public Long confirmItinerary(Long userId, Long itineraryId) {

        Itinerary newOne = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ItineraryException(
                        ErrorCode.ITINERARY_NOT_FOUND,
                        "itineraryId: " + itineraryId
                ));

        if (!newOne.getUserId().equals(userId)) {
            throw new ItineraryException(
                    ErrorCode.ITINERARY_FORBIDDEN,
                    "본인 일정만 저장할 수 있습니다. userId=" + userId
                            + ", ownerId=" + newOne.getUserId()
            );
        }

        LocalDate start = newOne.getStartDate();
        LocalDate end = newOne.getEndDate();

        // 겹치는 기존 일정 조회
        List<Itinerary> overlaps = itineraryRepository
                .findByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        userId, end, start);

        // 새로 확정하는 일정(itineraryId) 제외하고 삭제
        for (Itinerary it : overlaps) {
            if (!it.getId().equals(itineraryId)) {
                itineraryRepository.delete(it);
            }
        }

        newOne.setStatus(ItineraryStatus.ACTIVE);
        itineraryRepository.save(newOne);

        return newOne.getId();
    }

    // ========================================================
    // 3) 일정 생성
    //   - 과거 날짜 생성 불가
    //   - 종료일 < 시작일 방지
    //   - 최대 14일 제한
    //   - transportTypes를 동선 계산에 반영
    //   - Day1 마지막 장소 기준 호텔 추천 시도
    //   - B안: 장소/거리/항공/숙소 후보만 AI에 전달
    // ========================================================
    @Transactional
    public Long generateItinerary(
            ItineraryGenerationRequestDto req,
            Long userId
    ) {
        log.info("=== 🧭 일정 생성 시작 (userId:{}) ===", userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ItineraryException(
                        ErrorCode.USER_NOT_FOUND,
                        "userId: " + userId
                ));

        // 1) 날짜 파싱
        LocalDate start = LocalDate.parse(req.getStartDate());
        LocalDate end = LocalDate.parse(req.getEndDate());
        LocalDate today = LocalDate.now();

        // 2) 과거 날짜 검증
        if (start.isBefore(today)) {
            log.warn("과거 날짜로 일정 생성 시도: start={}, today={}", start, today);
            throw new ItineraryException(
                    ErrorCode.INVALID_DATE_RANGE,
                    "과거 날짜로는 일정을 생성할 수 없습니다."
            );
        }

        // 3) 종료일 < 시작일 방지
        if (end.isBefore(start)) {
            log.warn("종료일이 시작일보다 이전입니다. start={}, end={}", start, end);
            throw new ItineraryException(
                    ErrorCode.INVALID_DATE_RANGE,
                    "종료일은 시작일보다 이전일 수 없습니다."
            );
        }

        // 4) 최대 여행일 수 제한 (14일)
        int totalDays = (int) ChronoUnit.DAYS.between(start, end) + 1;
        if (totalDays > 14) {
            throw new ItineraryException(
                    ErrorCode.DATE_TOO_FAR,
                    "최대 14일까지 일정만 생성할 수 있습니다."
            );
        }

        // 5) 스케줄 밀도
        ScheduleDensity density =
                (req.getScheduleDensity() != null)
                        ? req.getScheduleDensity()
                        : ScheduleDensity.RELAXED;

        // 6) 사용자 이동수단 → Distance API 모드로 변환
        String userMode = resolveUserTransportMode(req.getTransportTypes());
        log.info("🚇 동선 계산용 이동 모드: {}", userMode);

        // 7) 일자별 예산
        Map<Integer, Integer> dailyBudgetMap =
                buildDailyBudgetMap(req.getBudget(), totalDays);

        // 8) 날씨 (위/경도 기반)
        List<WeatherInfoDto> weatherList =
                weatherService.getWeather(
                        req.getStartDate(),
                        req.getEndDate(),
                        req.getDestLat(),
                        req.getDestLng()
                );

        // 9) 장소 (날씨 + 스타일 + 거리 기반 필터) → 후보 리스트
        List<PlaceDto> placeList =
                googlePlacesService.getPlaces(
                        req.getDestinationCity(),
                        req.getDestLat(),
                        req.getDestLng(),
                        req.getTravelStyles(),   // ["ACTIVE", "FOOD", ...]
                        weatherList
                );

        log.info("📍 최종 Place 후보 수: {}", placeList.size());
        for (PlaceDto p : placeList) {
            log.info("   - {} (rating={}, tags={})",
                    p.getName(), p.getRating(), p.getStyleTags());
        }

        // 10) 항공 추천 (현재는 더미 또는 실제 API 로직 사용)
        List<FlightRecommendationDto> flights = null;
        if (!Boolean.TRUE.equals(req.getHasTransportTicket())) {
            flights = flightRecommendationService.generateDummyFlights(req.getStartDate());
        }

        // 11) 호텔 추천 (Day1 근처 기준 → 없으면 도시 기준)
        List<HotelRecommendationDto> hotels = null;
        if (Boolean.TRUE.equals(req.getNeedsHotel())) {
            int hotelBudget = (req.getHotelBudget() != null) ? req.getHotelBudget() : 0;

            if (!placeList.isEmpty()) {
                PlaceDto first = placeList.get(0);
                log.info("🏨 호텔 검색 기준 장소: {}, lat={}, lng={}",
                        first.getName(), first.getLat(), first.getLng());

                List<HotelRecommendationDto> nearby =
                        hotelRecommendationService.getHotelsNear(
                                first.getLat(),
                                first.getLng(),
                                hotelBudget
                        );

                if (nearby != null && !nearby.isEmpty()) {
                    hotels = nearby;
                } else {
                    log.info("🏨 주변 호텔 결과 없음 → 도시 기반 검색으로 fallback");
                    hotels = hotelRecommendationService.getHotels(
                            req.getDestinationCity(),
                            hotelBudget
                    );
                }
            } else {
                log.info("🏨 장소 없음 → 도시 기반 호텔 검색 사용");
                hotels = hotelRecommendationService.getHotels(
                        req.getDestinationCity(),
                        hotelBudget
                );
            }
        }

        // 12) 일정 엔티티 저장(초안 상태)
        Itinerary itinerary = Itinerary.builder()
                .userId(userId)
                .title(buildTitle(req, totalDays))
                .destinationCity(req.getDestinationCity())
                .startDate(start)
                .endDate(end)
                .totalDays(totalDays)
                .totalBudget(req.getBudget())
                .totalSpent(0)
                .scheduleDensity(density)
                .status(ItineraryStatus.GENERATED)
                .build();

        itineraryRepository.save(itinerary);

        // 13) OpenAI 호출 (B안: 후보 데이터만 넘김)
        ItineraryAiResult aiResult = openAIService.generateWithCandidates(
                req,
                weatherList,
                placeList,
                flights,
                hotels,
                userMode
        );

        if (aiResult == null) {
            throw new ItineraryException(
                    ErrorCode.API_TIMEOUT,
                    "OpenAI 응답 실패"
            );
        }

        itinerary.setAiRawText(aiResult.getText());
        itinerary.setAiJson(aiResult.getJson());
        itineraryRepository.save(itinerary);

        // 14) JSON → Day/Activity 저장
        jsonParseService.parseAndSave(
                itinerary,
                aiResult.getJson(),
                aiResult.getDayTexts(),
                dailyBudgetMap
        );

        log.info("=== ✨ 일정 생성 완료: id={} ===", itinerary.getId());
        return itinerary.getId();
    }

    // ========================================================
    // 제목 생성
    // ========================================================
    private String buildTitle(ItineraryGenerationRequestDto req, int totalDays) {
        return "%s %d일 여행".formatted(req.getDestinationCity(), totalDays);
    }

    // ========================================================
    // 일자별 예산
    // ========================================================
    private Map<Integer, Integer> buildDailyBudgetMap(Integer totalBudget, int totalDays) {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        if (totalBudget == null || totalBudget == 0) {
            for (int d = 1; d <= totalDays; d++) map.put(d, 0);
            return map;
        }

        int base = totalBudget / totalDays;
        int remainder = totalDays == 0 ? 0 : totalBudget % totalDays;

        for (int d = 1; d <= totalDays; d++) {
            int daily = base;
            if (d <= remainder) daily++;
            map.put(d, daily);
        }
        return map;
    }

    // ========================================================
    // 🔹 transportTypes → Distance API 모드 변환
    // ========================================================
    private String resolveUserTransportMode(List<String> transportTypes) {
        if (transportTypes == null || transportTypes.isEmpty()) {
            return "SUBWAY";  // 기본: 대중교통 위주
        }

        Set<String> types = new HashSet<>();
        for (String t : transportTypes) {
            if (t != null) types.add(t.toUpperCase());
        }

        if (types.contains("SUBWAY") || types.contains("BUS")) {
            return "SUBWAY";        // DistanceService에서 transit으로 변환
        }

        if (types.contains("TAXI") || types.contains("CAR")) {
            return "driving";       // Distance Matrix mode=driving
        }

        if (types.contains("WALK") || types.contains("WALKING")) {
            return "walking";
        }

        return "SUBWAY";
    }
}
