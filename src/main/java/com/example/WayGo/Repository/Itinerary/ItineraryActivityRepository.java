package com.example.WayGo.Repository.Itinerary;

import com.example.WayGo.Entity.Itinerary.ItineraryActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItineraryActivityRepository extends JpaRepository<ItineraryActivity, Long> {

    List<ItineraryActivity> findByItineraryDayIdOrderBySequence(Long dayId);
}
