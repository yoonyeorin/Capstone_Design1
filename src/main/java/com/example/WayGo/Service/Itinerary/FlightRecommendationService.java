package com.example.WayGo.Service.Itinerary;

import com.example.WayGo.Dto.Itinerary.FlightRecommendationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlightRecommendationService {

    /**
     * 사용자가 항공권이 없을 때 (hasTransportTicket = false)
     * → 첫날 아침 9~10시 도착 조건에 맞는 "더미 항공 2~3개"를 생성한다.
     */
    public List<FlightRecommendationDto> generateDummyFlights(String startDate) {

        LocalDateTime arrivalTarget1 = LocalDateTime.parse(startDate + "T09:00");
        LocalDateTime arrivalTarget2 = LocalDateTime.parse(startDate + "T09:30");
        LocalDateTime arrivalTarget3 = LocalDateTime.parse(startDate + "T10:00");

        List<FlightRecommendationDto> list = new ArrayList<>();

        list.add(
                FlightRecommendationDto.builder()
                        .airline("아시아나항공")
                        .flightNumber("OZ101")
                        .departure(arrivalTarget1.minusHours(3))
                        .arrival(arrivalTarget1)
                        .duration("약 3시간")
                        .price(250000)
                        .currency("KRW")
                        .isDirect(true)
                        .build()
        );

        list.add(
                FlightRecommendationDto.builder()
                        .airline("대한항공")
                        .flightNumber("KE703")
                        .departure(arrivalTarget2.minusHours(2).minusMinutes(30))
                        .arrival(arrivalTarget2)
                        .duration("약 2시간 30분")
                        .price(270000)
                        .currency("KRW")
                        .isDirect(true)
                        .build()
        );

        list.add(
                FlightRecommendationDto.builder()
                        .airline("일본항공 JAL")
                        .flightNumber("JL95")
                        .departure(arrivalTarget3.minusHours(3).minusMinutes(15))
                        .arrival(arrivalTarget3)
                        .duration("약 3시간 15분")
                        .price(240000)
                        .currency("KRW")
                        .isDirect(true)
                        .build()
        );

        return list;
    }
}
