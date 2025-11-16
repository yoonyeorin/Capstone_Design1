package com.example.WayGo.Service.Itinerary;

import com.example.WayGo.Dto.Itinerary.*;
import com.example.WayGo.Entity.Itinerary.Itinerary;
import com.example.WayGo.Entity.Itinerary.ItineraryActivity;
import com.example.WayGo.Entity.Itinerary.ItineraryDay;
import com.example.WayGo.Repository.Itinerary.ItineraryActivityRepository;
import com.example.WayGo.Repository.Itinerary.ItineraryDayRepository;
import com.example.WayGo.Repository.Itinerary.ItineraryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 일정 조회 서비스
 * - 저장된 일정 → DTO로 변환하여 프론트에 전달
 */
@Service
@RequiredArgsConstructor
public class ItineraryQueryService {

    private final ItineraryRepository itineraryRepository;
    private final ItineraryDayRepository dayRepository;
    private final ItineraryActivityRepository activityRepository;

    // 전체 일정 조회
    public ItineraryResponseDto getItinerary(Long itineraryId) {

        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다"));

        // 일자 리스트 조회
        List<ItineraryDay> days = dayRepository.findByItineraryId(itineraryId);

        // 일자 DTO 변환
        List<ItineraryDayDto> dayDtos = days.stream()
                .map(this::convertToDayDto)
                .collect(Collectors.toList());

        return ItineraryResponseDto.builder()
                .itineraryId(itinerary.getId())
                .title(itinerary.getTitle())                     // ex) "도쿄 2박3일"
                .destinationCity(itinerary.getDestinationCity()) // 여행 도시
                .startDate(itinerary.getStartDate().toString())
                .endDate(itinerary.getEndDate().toString())
                .totalDays(itinerary.getTotalDays())
                .totalBudget(itinerary.getTotalBudget())
                .totalSpent(itinerary.getTotalSpent())
                .days(dayDtos)
                .build();
    }

    // Day Entity → Day DTO 변환
    private ItineraryDayDto convertToDayDto(ItineraryDay day) {

        List<ItineraryActivity> activities =
                activityRepository.findByItineraryDayIdOrderBySequence(day.getId());

        List<ItineraryActivityDto> activityDtos = activities.stream()
                .map(this::convertToActivityDto)
                .collect(Collectors.toList());

        return ItineraryDayDto.builder()
                .dayNumber(day.getDayNumber())                  // ex) 1일차
                .date(day.getDate().toString())                 // "2025-11-09"
                .weatherCondition(day.getWeatherCondition())
                .temperature(day.getTemperature())
                .weatherAdvice(day.getWeatherAdvice())
                .dailyBudget(day.getDailyBudget())
                .dailySpent(day.getDailySpent())
                .activities(activityDtos)
                .build();
    }

    // Activity Entity → Activity DTO (완성형)
    private ItineraryActivityDto convertToActivityDto(ItineraryActivity activity) {
        return ItineraryActivityDto.builder()
                .sequence(activity.getSequence())                         // 순서 1,2,3

                .activityType(activity.getActivityType() != null
                        ? activity.getActivityType().name()
                        : null)                                           // Enum → String

                .name(activity.getName())                                 // 활동명
                .address(activity.getAddress())                           // 주소

                .startTime(activity.getStartTime() != null
                        ? activity.getStartTime().toString()
                        : null)                                           // LocalTime → String

                .endTime(activity.getEndTime() != null
                        ? activity.getEndTime().toString()
                        : null)

                .durationMinutes(activity.getDurationMinutes())

                .transportType(activity.getTransportType() != null
                        ? activity.getTransportType().name()
                        : null)                                           // Enum → String

                .transportDuration(activity.getTransportDuration())
                .transportCost(activity.getTransportCost())

                .entranceFee(activity.getEntranceFee())
                .mealCost(activity.getMealCost())
                .rating(activity.getRating())
                .tip(activity.getTip())

                .build();
    }
}
