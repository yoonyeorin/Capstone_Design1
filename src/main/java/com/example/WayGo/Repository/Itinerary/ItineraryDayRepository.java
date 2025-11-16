package com.example.WayGo.Repository.Itinerary;

import com.example.WayGo.Entity.Itinerary.ItineraryDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItineraryDayRepository extends JpaRepository<ItineraryDay, Long> {
    List<ItineraryDay> findByItineraryId(Long itineraryId);
}
