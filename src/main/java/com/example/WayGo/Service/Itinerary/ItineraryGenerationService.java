package com.example.WayGo.Service.Itinerary;

import com.example.WayGo.Dto.Itinerary.ItineraryGenerationRequestDto;
import com.example.WayGo.Entity.Itinerary.Itinerary;
import com.example.WayGo.Entity.Itinerary.ItineraryDay;
import com.example.WayGo.Repository.Itinerary.ItineraryActivityRepository;
import com.example.WayGo.Repository.Itinerary.ItineraryDayRepository;
import com.example.WayGo.Repository.Itinerary.ItineraryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 일정 생성 서비스 (AI + 날씨 + 장소 붙기 전 기본 틀)
 */
@Service
@RequiredArgsConstructor
public class ItineraryGenerationService {

    private final ItineraryRepository itineraryRepository;
    private final ItineraryDayRepository dayRepository;
    private final ItineraryActivityRepository activityRepository;

    /** 일정 생성 (기본 틀) */
    public Long generateItinerary(ItineraryGenerationRequestDto req) {

        // 1) 일정 엔티티 생성
        Itinerary itinerary = createItinerary(req);
        itineraryRepository.save(itinerary);

        // 2) Day 엔티티 생성
        List<ItineraryDay> days = createDays(itinerary, req);
        dayRepository.saveAll(days);

        // 3) AI 일정 생성 부분 → 다음 단계에서 구현
        // String aiResult = openAiService.generateItinerary(req, days);

        // 4) AI 결과 → 활동(Activity) 저장 → 다음 단계에서 구현

        return itinerary.getId();
    }

    /** Itinerary 생성 */
    private Itinerary createItinerary(ItineraryGenerationRequestDto req) {

        LocalDate start = LocalDate.parse(req.getStartDate());
        LocalDate end = LocalDate.parse(req.getEndDate());

        int totalDays = (int) (end.toEpochDay() - start.toEpochDay() + 1);

        return Itinerary.builder()
                .title(req.getDestinationCity() + " 일정")
                .destinationCity(req.getDestinationCity())
                .startDate(start)
                .endDate(end)
                .totalDays(totalDays)
                .totalBudget(req.getBudget())
                .totalSpent(0)
                .build();
    }

    /** 날짜별 Day 엔티티 생성 */
    private List<ItineraryDay> createDays(Itinerary itinerary, ItineraryGenerationRequestDto req) {

        LocalDate start = itinerary.getStartDate();
        LocalDate end = itinerary.getEndDate();

        List<ItineraryDay> result = new ArrayList<>();
        int idx = 1;

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            result.add(ItineraryDay.builder()
                    .itinerary(itinerary)
                    .dayNumber(idx++)
                    .date(d)
                    .weatherCondition(null)     // 추후 AI + 날씨 API로 채울 예정
                    .temperature(null)
                    .weatherAdvice(null)
                    .dailyBudget(0)
                    .dailySpent(0)
                    .build());
        }

        return result;
    }
}
