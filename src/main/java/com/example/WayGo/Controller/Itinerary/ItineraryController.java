package com.example.WayGo.Controller.Itinerary;

import com.example.WayGo.Dto.Itinerary.ItineraryGenerationRequestDto;
import com.example.WayGo.Dto.Itinerary.ItineraryResponseDto;
import com.example.WayGo.Service.Itinerary.ItineraryGenerationService;
import com.example.WayGo.Service.Itinerary.ItineraryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/itinerary")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryGenerationService itineraryGenerationService;
    private final ItineraryQueryService itineraryQueryService;

    /** 일정 생성 API */
    @PostMapping("/generate")
    public ResponseEntity<Long> generate(@RequestBody ItineraryGenerationRequestDto request) {
        Long id = itineraryGenerationService.generateItinerary(request);
        return ResponseEntity.ok(id);
    }

    /** 일정 조회 API */
    @GetMapping("/{id}")
    public ResponseEntity<ItineraryResponseDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(itineraryQueryService.getItinerary(id));
    }
}
