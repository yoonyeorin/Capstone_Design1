package com.example.WayGo.Repository.Itinerary;

import com.example.WayGo.Entity.ItineraryActivity;
import com.example.WayGo.Entity.ItineraryDay;
import com.example.WayGo.Entity.enums.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ItineraryActivity 데이터베이스 접근 인터페이스
 */
@Repository
public interface ItineraryActivityRepository extends JpaRepository<ItineraryActivity, Long> {

    /**
     * 특정 일차의 모든 활동 조회 (순서대로)
     */
    List<ItineraryActivity> findByItineraryDayOrderBySequenceAsc(ItineraryDay itineraryDay);

    /**
     * 특정 일차의 특정 타입 활동만 조회
     */
    List<ItineraryActivity> findByItineraryDayAndActivityType(
            ItineraryDay itineraryDay,
            ActivityType activityType
    );

    /**
     * 특정 일차의 활동 개수
     */
    long countByItineraryDay(ItineraryDay itineraryDay);

    /**
     * 특정 일차의 총 지출 계산
     */
    @Query("""
        SELECT COALESCE(SUM(a.entranceFee + a.mealCost + a.transportCost), 0)
        FROM ItineraryActivity a
        WHERE a.itineraryDay = :day
    """)
    Integer calculateDailySpent(@Param("day") ItineraryDay day);

    /**
     * 특정 일차의 마지막 활동 조회
     */
    @Query("""
        SELECT a FROM ItineraryActivity a
        WHERE a.itineraryDay = :day
        ORDER BY a.sequence DESC
        LIMIT 1
    """)
    ItineraryActivity findLastActivityOfDay(@Param("day") ItineraryDay day);
}