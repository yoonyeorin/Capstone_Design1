package com.example.WayGo.Repository.Itinerary;

import com.example.WayGo.Entity.Itinerary;
import com.example.WayGo.Entity.ItineraryDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ItineraryDay 데이터베이스 접근 인터페이스
 */
@Repository
public interface ItineraryDayRepository extends JpaRepository<ItineraryDay, Long> {

    /**
     * 특정 일정의 모든 일별 일정 조회 (날짜순)
     */
    List<ItineraryDay> findByItineraryOrderByDayNumberAsc(Itinerary itinerary);

    /**
     * 특정 일정의 특정 날짜 조회
     */
    Optional<ItineraryDay> findByItineraryAndDate(Itinerary itinerary, LocalDate date);

    /**
     * 특정 일정의 특정 일차 조회
     */
    Optional<ItineraryDay> findByItineraryAndDayNumber(Itinerary itinerary, Integer dayNumber);

    /**
     * 특정 일정의 일별 일정 개수
     */
    long countByItinerary(Itinerary itinerary);

    /**
     * 특정 일정의 총 예상 지출 계산
     */
    @Query("""
        SELECT COALESCE(SUM(d.dailySpent), 0)
        FROM ItineraryDay d
        WHERE d.itinerary = :itinerary
    """)
    Integer calculateTotalSpent(@Param("itinerary") Itinerary itinerary);
}