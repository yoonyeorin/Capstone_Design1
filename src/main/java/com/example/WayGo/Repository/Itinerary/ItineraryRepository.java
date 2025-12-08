package com.example.WayGo.Repository.Itinerary;

import com.example.WayGo.Entity.Itinerary.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.WayGo.Entity.enums.ItineraryStatus;

import java.time.LocalDate;
import java.util.List;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {

    List<Itinerary> findByUserId(Long userId);

    // ⭐ 특정 유저의 겹치는 일정들 찾기 (상태까지 필터)
    List<Itinerary> findByUserIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long userId,
            ItineraryStatus status,
            LocalDate newEnd,
            LocalDate newStart
    );

    // ⭐ 자기 자신(itineraryId)은 제외하고 겹치는 일정 찾기
    List<Itinerary> findByUserIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
            Long userId,
            ItineraryStatus status,
            LocalDate newEnd,
            LocalDate newStart,
            Long excludeId
    );

    List<Itinerary> findByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long userId,
            LocalDate newEnd,
            LocalDate newStart
    );
}

