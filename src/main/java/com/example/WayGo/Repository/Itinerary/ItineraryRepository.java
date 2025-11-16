package com.example.WayGo.Repository.Itinerary;

import com.example.WayGo.Entity.Itinerary.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {

    List<Itinerary> findByUserId(Long userId);

    boolean existsByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long userId,
            LocalDate newEnd,
            LocalDate newStart
    );
}
