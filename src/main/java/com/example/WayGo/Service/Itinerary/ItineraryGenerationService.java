package com.example.WayGo.Service.Itinerary;

import com.example.WayGo.Dto.Itinerary.*;
import com.example.WayGo.Entity.*;
import com.example.WayGo.Entity.enums.*;
import com.example.WayGo.Exception.InputNotFoundException;
import com.example.WayGo.Repository.Itinerary.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 일정 생성 알고리즘 Service
 *
 * 8단계 입력을 기반으로 실제 여행 일정을 생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ItineraryGenerationService {

    private final ItineraryInputRepository inputRepository;
    private final ItineraryRepository itineraryRepository;
    private final ItineraryDayRepository dayRepository;
    private final ItineraryActivityRepository activityRepository;

    // TODO: 나중에 추가할 서비스들
    // private final PlaceRecommendationService placeRecommendationService;
    // private final WeatherService weatherService;
    // private final DistanceCalculationService distanceService;

    /**
     * 일정 생성 메인 메서드
     *
     * @param inputId 8단계 입력 ID
     * @return 생성된 일정 ID
     */
    public Long generateItinerary(Integer inputId) {
        log.info("=== 일정 생성 시작: inputId={} ===", inputId);

        // 1. 입력 데이터 조회 & 검증
        ItineraryInput input = validateAndGetInput(inputId);

        // 2. 제목 생성 & Itinerary 객체 생성
        Itinerary itinerary = createItinerary(input);

        // 3. 일별 일정 생성
        generateDailyItineraries(itinerary, input);

        // 4. 총 지출 계산 & 저장
        calculateAndSaveTotalSpent(itinerary);

        // 5. 입력 상태 업데이트
        input.setStatus(InputStatus.GENERATED);
        inputRepository.save(input);

        log.info("=== 일정 생성 완료: itineraryId={} ===", itinerary.getId());

        return itinerary.getId();
    }

    /**
     * 입력 데이터 검증
     */
    private ItineraryInput validateAndGetInput(Integer inputId) {
        ItineraryInput input = inputRepository.findById(inputId)
                .orElseThrow(() -> new InputNotFoundException("입력을 찾을 수 없습니다"));

        if (input.getStatus() != InputStatus.COMPLETED) {
            throw new IllegalStateException("8단계 입력이 완료되지 않았습니다");
        }

        log.info("입력 검증 완료: 도시={}, {}박{}일",
                input.getDestinationCity(),
                input.getTotalDays() - 1,
                input.getTotalDays());

        return input;
    }

    /**
     * Itinerary 객체 생성
     */
    private Itinerary createItinerary(ItineraryInput input) {
        String title = generateTitle(input);

        Itinerary itinerary = new Itinerary();
        itinerary.setItineraryInput(input);
        itinerary.setUser(input.getUser());
        itinerary.setTitle(title);
        itinerary.setTotalBudget(input.getBudget());
        itinerary.setTotalSpent(0);
        itinerary.setStatus(ItineraryStatus.ACTIVE);

        return itineraryRepository.save(itinerary);
    }

    /**
     * 제목 자동 생성
     * 예: "도쿄 2박 3일"
     */
    private String generateTitle(ItineraryInput input) {
        int nights = input.getTotalDays() - 1;
        int days = input.getTotalDays();

        return String.format("%s %d박 %d일",
                input.getDestinationCity(),
                nights,
                days);
    }

    /**
     * 일별 일정 생성 (메인 루프)
     */
    private void generateDailyItineraries(Itinerary itinerary, ItineraryInput input) {
        log.info("일별 일정 생성 시작");

        LocalDate currentDate = input.getStartDate();
        int dailyBudget = input.getBudget() / input.getTotalDays();

        for (int day = 1; day <= input.getTotalDays(); day++) {
            log.info("{}일차 일정 생성 중: {}", day, currentDate);

            // 일별 일정 객체 생성
            ItineraryDay itineraryDay = createItineraryDay(
                    itinerary,
                    day,
                    currentDate,
                    dailyBudget
            );

            // TODO: 날씨 정보 가져오기
            // fetchAndSetWeather(itineraryDay, currentDate);

            // TODO: 활동 생성 (알고리즘 핵심)
            // generateActivities(itineraryDay, input, day);

            // 임시: 기본 활동 2개 추가
            addTemporaryActivities(itineraryDay, input, day);

            // 하루 지출 계산
            calculateDailySpent(itineraryDay);

            currentDate = currentDate.plusDays(1);
        }

        log.info("일별 일정 생성 완료: 총 {}일", input.getTotalDays());
    }

    /**
     * ItineraryDay 객체 생성
     */
    private ItineraryDay createItineraryDay(
            Itinerary itinerary,
            int dayNumber,
            LocalDate date,
            int dailyBudget) {

        ItineraryDay day = new ItineraryDay();
        day.setItinerary(itinerary);
        day.setDayNumber(dayNumber);
        day.setDate(date);
        day.setDailyBudget(dailyBudget);
        day.setDailySpent(0);

        // TODO: 날씨 정보는 나중에
        day.setWeatherCondition("맑음");
        day.setTemperature(22);
        day.setWeatherAdvice("좋은 날씨입니다!");

        return dayRepository.save(day);
    }

    /**
     * 임시 활동 추가 (테스트용)
     * TODO: 실제 알고리즘으로 대체
     */
    private void addTemporaryActivities(ItineraryDay day, ItineraryInput input, int dayNumber) {
        log.info("임시 활동 생성 중...");

        // 시작 시간 결정
        LocalTime startTime = (dayNumber == 1 && input.getArrivalTime() != null)
                ? input.getArrivalTime()
                : LocalTime.of(input.getScheduleDensity().getStartHour(), 0);

        // 활동 1: 관광지
        ItineraryActivity place1 = new ItineraryActivity();
        place1.setItineraryDay(day);
        place1.setSequence(1);
        place1.setActivityType(ActivityType.PLACE);
        place1.setPlaceName("테스트 관광지 " + dayNumber);
        place1.setStartTime(startTime);
        place1.setEndTime(startTime.plusHours(2));
        place1.setDurationMinutes(120);
        place1.setEntranceFee(10000);
        place1.setTransportToNext("SUBWAY");
        place1.setTransportDuration(20);
        place1.setTransportCost(1500);
        activityRepository.save(place1);

        // 활동 2: 점심
        ItineraryActivity meal = new ItineraryActivity();
        meal.setItineraryDay(day);
        meal.setSequence(2);
        meal.setActivityType(ActivityType.MEAL);
        meal.setPlaceName("테스트 식당 " + dayNumber);
        meal.setStartTime(LocalTime.of(12, 0));
        meal.setEndTime(LocalTime.of(13, 0));
        meal.setDurationMinutes(60);
        meal.setMealCost(15000);
        activityRepository.save(meal);

        log.info("임시 활동 2개 생성 완료");
    }

    /**
     * 하루 지출 계산
     */
    private void calculateDailySpent(ItineraryDay day) {
        Integer spent = activityRepository.calculateDailySpent(day);
        day.setDailySpent(spent);
        dayRepository.save(day);

        log.info("{}일차 지출 계산: {}원", day.getDayNumber(), spent);
    }

    /**
     * 총 지출 계산 & 저장
     */
    private void calculateAndSaveTotalSpent(Itinerary itinerary) {
        Integer totalSpent = dayRepository.calculateTotalSpent(itinerary);
        itinerary.setTotalSpent(totalSpent);
        itineraryRepository.save(itinerary);

        log.info("총 지출: {}원 (예산: {}원)",
                totalSpent,
                itinerary.getTotalBudget());
    }

    /**
     * 생성된 일정 조회
     */
    @Transactional(readOnly = true)
    public ItineraryResponseDto getItinerary(Long itineraryId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다"));

        return convertToResponseDto(itinerary);
    }

    /**
     * Entity → DTO 변환
     */
    private ItineraryResponseDto convertToResponseDto(Itinerary itinerary) {
        List<ItineraryDay> days = dayRepository.findByItineraryOrderByDayNumberAsc(itinerary);

        List<ItineraryDayDto> dayDtos = days.stream()
                .map(this::convertToDayDto)
                .collect(Collectors.toList());

        return ItineraryResponseDto.builder()
                .itineraryId(itinerary.getId())
                .userId(itinerary.getUser().getId())
                .title(itinerary.getTitle())
                .totalBudget(itinerary.getTotalBudget())
                .totalSpent(itinerary.getTotalSpent())
                .status(itinerary.getStatus().name())
                .days(dayDtos)
                .createdAt(itinerary.getCreatedAt())
                .updatedAt(itinerary.getUpdatedAt())
                .build();
    }

    private ItineraryDayDto convertToDayDto(ItineraryDay day) {
        List<ItineraryActivity> activities =
                activityRepository.findByItineraryDayOrderBySequenceAsc(day);

        List<ItineraryActivityDto> activityDtos = activities.stream()
                .map(this::convertToActivityDto)
                .collect(Collectors.toList());

        return ItineraryDayDto.builder()
                .dayId(day.getId())
                .dayNumber(day.getDayNumber())
                .date(day.getDate())
                .weatherCondition(day.getWeatherCondition())
                .temperature(day.getTemperature())
                .weatherAdvice(day.getWeatherAdvice())
                .dailyBudget(day.getDailyBudget())
                .dailySpent(day.getDailySpent())
                .activities(activityDtos)
                .build();
    }

    private ItineraryActivityDto convertToActivityDto(ItineraryActivity activity) {
        return ItineraryActivityDto.builder()
                .activityId(activity.getId())
                .sequence(activity.getSequence())
                .activityType(activity.getActivityType().name())
                .placeName(activity.getPlaceName())
                .placeId(activity.getPlaceId())
                .address(activity.getAddress())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .durationMinutes(activity.getDurationMinutes())
                .entranceFee(activity.getEntranceFee())
                .mealCost(activity.getMealCost())
                .rating(activity.getRating())
                .transportToNext(activity.getTransportToNext())
                .transportDuration(activity.getTransportDuration())
                .transportCost(activity.getTransportCost())
                .tips(activity.getTips())
                .build();
    }
}