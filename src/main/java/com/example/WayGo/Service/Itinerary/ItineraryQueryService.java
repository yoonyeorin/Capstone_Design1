package com.example.WayGo.Service.Itinerary;

import com.example.WayGo.Dto.Itinerary.ItineraryActivityDto;
import com.example.WayGo.Dto.Itinerary.ItineraryAiRawResponseDto;
import com.example.WayGo.Dto.Itinerary.ItineraryCalendarSummaryDto;
import com.example.WayGo.Dto.Itinerary.ItineraryDayDto;
import com.example.WayGo.Dto.Itinerary.ItineraryDayTextDto;
import com.example.WayGo.Dto.Itinerary.ItineraryResponseDto;
import com.example.WayGo.Dto.Itinerary.ItinerarySummaryDto;
import com.example.WayGo.Dto.Itinerary.ItineraryTextResponseDto;
import com.example.WayGo.Entity.Itinerary.Itinerary;
import com.example.WayGo.Entity.Itinerary.ItineraryActivity;
import com.example.WayGo.Entity.Itinerary.ItineraryDay;
import com.example.WayGo.Entity.enums.ActivityType;
import com.example.WayGo.Entity.enums.ItineraryStatus;
import com.example.WayGo.Repository.Itinerary.ItineraryActivityRepository;
import com.example.WayGo.Repository.Itinerary.ItineraryDayRepository;
import com.example.WayGo.Repository.Itinerary.ItineraryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItineraryQueryService {

    private final ItineraryRepository itineraryRepository;
    private final ItineraryDayRepository dayRepository;
    private final ItineraryActivityRepository activityRepository;


    // ========================================================
    // 1) 일정 상세 조회 (확정 ACTIVE + 임시 GENERATED 모두 조회)
    // ========================================================
    public ItineraryResponseDto getItinerary(Long itineraryId) {

        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다."));

        List<ItineraryDay> days = dayRepository.findByItineraryId(itineraryId);

        List<ItineraryDayDto> dayDtos = days.stream()
                .map(this::convertToDayDto)
                .collect(Collectors.toList());

        return ItineraryResponseDto.builder()
                .itineraryId(itinerary.getId())
                .title(itinerary.getTitle())
                .destinationCity(itinerary.getDestinationCity())
                .startDate(itinerary.getStartDate().toString())
                .endDate(itinerary.getEndDate().toString())
                .totalDays(itinerary.getTotalDays())
                .totalBudget(itinerary.getTotalBudget())
                .totalSpent(itinerary.getTotalSpent())
                .days(dayDtos)
                .build();
    }


    // ========================================================
    // 2) AI 원본 텍스트 조회 (TEXT 뷰용: dayHeader + dayContent만)
    // ========================================================
    public ItineraryTextResponseDto getAiFormattedText(Long itineraryId) {

        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다."));

        String aiRawText = itinerary.getAiRawText();

        if (aiRawText == null || aiRawText.isBlank()) {
            throw new RuntimeException("AI 원문이 없습니다.");
        }

        // AI 텍스트를 날짜별로 파싱해서 ItineraryDayTextDto 리스트로 변환
        List<ItineraryDayTextDto> parsedDays = parseAiTextToDayTextDtos(aiRawText);

        return ItineraryTextResponseDto.builder()
                .itineraryId(itinerary.getId())
                .title(itinerary.getTitle())
                .destinationCity(itinerary.getDestinationCity())
                .startDate(itinerary.getStartDate().toString())
                .endDate(itinerary.getEndDate().toString())
                .totalDays(itinerary.getTotalDays())
                .totalBudget(itinerary.getTotalBudget())
                .totalSpent(itinerary.getTotalSpent())
                .days(parsedDays)  // dayHeader, dayContent만 들어있는 DTO들
                .build();
    }


    // ========================================================
    // 3) AI 원본 텍스트 조회 (RAW - 파싱 안 함)
    // ========================================================
    public ItineraryAiRawResponseDto getAiRawText(Long itineraryId) {

        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다."));

        return ItineraryAiRawResponseDto.builder()
                .itineraryId(itinerary.getId())
                .aiRawText(itinerary.getAiRawText())
                .build();
    }


    // ========================================================
    // 4) 단일 일정 캘린더 요약 (1일차~N일차)
    // ========================================================
    public List<ItineraryCalendarSummaryDto> getCalendarSummary(Long itineraryId) {

        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다"));

        List<ItineraryDay> days = dayRepository.findByItineraryId(itineraryId);

        List<ItineraryCalendarSummaryDto> result = new ArrayList<>();

        // 일정 밀도 (RELAXED=2, PACKED=4 등)
        int maxPlaces = itinerary.getScheduleDensity().getTargetPlacesPerDay();

        for (ItineraryDay day : days) {

            List<ItineraryActivity> activities =
                    activityRepository.findByItineraryDayIdOrderBySequence(day.getId());

            List<String> placeNames = activities.stream()
                    .filter(a -> a.getActivityType() == ActivityType.PLACE)
                    .map(ItineraryActivity::getName)
                    .limit(maxPlaces)
                    .collect(Collectors.toList());

            result.add(
                    ItineraryCalendarSummaryDto.builder()
                            .date(day.getDate().toString())
                            .mainPlaces(placeNames)
                            .build()
            );
        }

        return result;
    }


    // ========================================================
    // 5) 월간 캘린더 (사용자의 여러 일정)
    // ========================================================
    public List<ItinerarySummaryDto> getCalendarSummary(Long userId, int year, int month) {

        List<Itinerary> itineraries = itineraryRepository.findByUserId(userId);

        List<ItinerarySummaryDto> result = new ArrayList<>();

        for (Itinerary it : itineraries) {

            if (it.getStatus() != ItineraryStatus.ACTIVE) continue;

            List<ItineraryDay> days = dayRepository.findByItineraryId(it.getId());

            for (ItineraryDay day : days) {

                if (day.getDate().getYear() != year ||
                        day.getDate().getMonthValue() != month)
                    continue;

                List<ItineraryActivity> acts =
                        activityRepository.findByItineraryDayIdOrderBySequence(day.getId());

                List<String> mainPlaces = acts.stream()
                        .filter(a -> a.getName() != null)
                        .limit(4)
                        .map(ItineraryActivity::getName)
                        .toList();

                result.add(
                        ItinerarySummaryDto.builder()
                                .itineraryId(it.getId())
                                .date(day.getDate().toString())
                                .scheduleDensity(it.getScheduleDensity().name())
                                .mainPlaces(mainPlaces)
                                .build()
                );
            }
        }

        return result;
    }


    // ========================================================
    // Private 헬퍼 메서드들
    // ========================================================

    /**
     * AI TEXT 원문을 "📅 N일차 – ..." 기준으로 잘라서
     * TEXT 뷰 전용 ItineraryDayTextDto 리스트로 변환
     */
    private List<ItineraryDayTextDto> parseAiTextToDayTextDtos(String aiRawText) {

        List<ItineraryDayTextDto> result = new ArrayList<>();

        if (aiRawText == null || aiRawText.isBlank()) {
            return result;
        }

        String[] lines = aiRawText.split("\\R");

        int dayNumber = 0;
        String currentHeader = null;
        StringBuilder currentContent = new StringBuilder();

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            // 구분선(━━━━━━━━) 같은 건 버리기
            if (line.startsWith("━")) {
                continue;
            }

            // 🔹 Markdown 헤더(#, ##, ### ...) 제거
            while (line.startsWith("#")) {
                line = line.substring(1).trim();
            }

            // 🔹 이제 "📅" 로 시작하면 새 일차
            if (line.startsWith("📅")) {

                // 이전 일차 flush
                if (currentHeader != null) {
                    result.add(
                            ItineraryDayTextDto.builder()
                                    .dayNumber(dayNumber)
                                    .dayHeader(currentHeader)
                                    .dayContent(currentContent.toString().trim())
                                    .build()
                    );
                }

                // 새 일차 시작
                dayNumber++;
                currentHeader = line;      // "# "가 제거된 "📅 1일차 – ..." 그대로 저장
                currentContent.setLength(0);
                continue;
            }

            // 헤더가 세팅된 이후의 라인들은 내용으로 누적
            if (currentHeader != null) {
                currentContent.append(line).append("\n");
            }
        }

        // 마지막 일차 flush
        if (currentHeader != null) {
            result.add(
                    ItineraryDayTextDto.builder()
                            .dayNumber(dayNumber)
                            .dayHeader(currentHeader)
                            .dayContent(currentContent.toString().trim())
                            .build()
            );
        }

        log.info("✅ AI 텍스트 파싱 완료(TEXT 뷰): {}개 일차", result.size());

        return result;
    }



    /**
     * Day Entity → 상세 화면용 DTO 변환
     */
    private ItineraryDayDto convertToDayDto(ItineraryDay day) {

        List<ItineraryActivity> activities =
                activityRepository.findByItineraryDayIdOrderBySequence(day.getId());

        List<ItineraryActivityDto> activityDtos = activities.stream()
                .map(this::convertToActivityDto)
                .collect(Collectors.toList());

        return ItineraryDayDto.builder()
                .dayNumber(day.getDayNumber())
                .date(day.getDate().toString())
                .weatherCondition(day.getWeatherCondition())
                .temperature(day.getTemperature())
                .weatherAdvice(day.getWeatherAdvice())
                .dailyBudget(day.getDailyBudget())
                .dailySpent(day.getDailySpent())
                .dayText(day.getDayText())
                .activities(activityDtos)
                .build();
    }


    /**
     * Activity Entity → DTO 변환
     */
    private ItineraryActivityDto convertToActivityDto(ItineraryActivity activity) {
        return ItineraryActivityDto.builder()
                .sequence(activity.getSequence())
                .activityType(activity.getActivityType() != null
                        ? activity.getActivityType().name()
                        : null)
                .name(activity.getName())
                .address(activity.getAddress())
                .startTime(activity.getStartTime() != null ? activity.getStartTime().toString() : null)
                .endTime(activity.getEndTime() != null ? activity.getEndTime().toString() : null)
                .durationMinutes(activity.getDurationMinutes())
                .transportType(activity.getTransportType() != null
                        ? activity.getTransportType().name()
                        : null)
                .transportDuration(activity.getTransportDuration())
                .transportCost(activity.getTransportCost())
                .entranceFee(activity.getEntranceFee())
                .mealCost(activity.getMealCost())
                .rating(activity.getRating())
                .tip(activity.getTip())
                .build();
    }
}
