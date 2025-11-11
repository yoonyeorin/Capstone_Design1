package com.example.WayGo.Controller.Itinerary;

import com.example.WayGo.Dto.Itinerary.*;
import com.example.WayGo.Service.Itinerary.ItineraryInputService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 일정 입력 컨트롤러
 *
 * ⚠️ ID 타입: Integer
 */
@RestController
@RequestMapping("/api/itinerary/input")
@RequiredArgsConstructor
@Slf4j
public class ItineraryInputController {

    private final ItineraryInputService itineraryInputService;

    // ============================================================
    // Step 1: 도시 검색 & 선택
    // ============================================================

    @PostMapping("/step1/search-city")
    public ResponseEntity<List<Step1ResponseDto>> searchCity(
            @RequestBody @Valid Step1RequestDto request) {

        log.info("도시 검색 API 호출: {}", request.getCityName());

        List<Step1ResponseDto> results = itineraryInputService.searchCity(request);

        return ResponseEntity.ok(results);
    }

    @PostMapping("/step1/select")
    public ResponseEntity<Integer> selectCity(  // Long → Integer
                                                @AuthenticationPrincipal UserDetails userDetails,
                                                @RequestBody @Valid Step1ResponseDto cityInfo) {

        log.info("도시 선택 API 호출: 사용자={}, 도시={}",
                userDetails.getUsername(), cityInfo.getCityName());

        Integer inputId = itineraryInputService.saveStep1(  // Long → Integer
                userDetails.getUsername(),
                cityInfo
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(inputId);
    }

    // ============================================================
    // Step 2: 날짜 선택
    // ============================================================

    @PutMapping("/{inputId}/step2")
    public ResponseEntity<String> saveStep2(
            @PathVariable Integer inputId,  // Long → Integer
            @RequestBody @Valid Step2RequestDto request) {

        log.info("Step 2 API 호출: inputId={}, 기간={} ~ {}",
                inputId, request.getStartDate(), request.getEndDate());

        itineraryInputService.saveStep2(inputId, request);

        return ResponseEntity.ok("Step 2 저장 완료");
    }

    // ============================================================
    // Step 3: 교통편
    // ============================================================

    @PutMapping("/{inputId}/step3")
    public ResponseEntity<?> saveStep3(
            @PathVariable Integer inputId,  // Long → Integer
            @RequestBody @Valid Step3RequestDto request) {

        log.info("Step 3 API 호출: inputId={}, hasTicket={}",
                inputId, request.getHasTicket());

        List<FlightRecommendationDto> flights =
                itineraryInputService.saveStep3(inputId, request);

        if (flights == null || flights.isEmpty()) {
            return ResponseEntity.ok("Step 3 저장 완료");
        }

        return ResponseEntity.ok(flights);
    }

    // ============================================================
    // Step 4: 인원수
    // ============================================================

    @PutMapping("/{inputId}/step4")
    public ResponseEntity<String> saveStep4(
            @PathVariable Integer inputId,  // Long → Integer
            @RequestBody @Valid Step4RequestDto request) {

        log.info("Step 4 API 호출: inputId={}, 인원={}",
                inputId, request.getNumberOfPeople());

        itineraryInputService.saveStep4(inputId, request);

        return ResponseEntity.ok("Step 4 저장 완료");
    }

    // ============================================================
    // Step 5: 이동수단
    // ============================================================

    @PutMapping("/{inputId}/step5")
    public ResponseEntity<String> saveStep5(
            @PathVariable Integer inputId,  // Long → Integer
            @RequestBody @Valid Step5RequestDto request) {

        log.info("Step 5 API 호출: inputId={}, 교통수단={}",
                inputId, request.getTransportTypes());

        itineraryInputService.saveStep5(inputId, request);

        return ResponseEntity.ok("Step 5 저장 완료");
    }

    // ============================================================
    // Step 6: 여행 취향 + 일정 밀도
    // ============================================================

    @PutMapping("/{inputId}/step6")
    public ResponseEntity<String> saveStep6(
            @PathVariable Integer inputId,  // Long → Integer
            @RequestBody @Valid Step6RequestDto request) {

        log.info("Step 6 API 호출: inputId={}, 취향={}, 밀도={}",
                inputId, request.getTravelStyles(), request.getScheduleDensity());

        itineraryInputService.saveStep6(inputId, request);

        return ResponseEntity.ok("Step 6 저장 완료");
    }

    // ============================================================
    // Step 7: 예산
    // ============================================================

    @PutMapping("/{inputId}/step7")
    public ResponseEntity<String> saveStep7(
            @PathVariable Integer inputId,  // Long → Integer
            @RequestBody @Valid Step7RequestDto request) {

        log.info("Step 7 API 호출: inputId={}, 예산={}",
                inputId, request.getBudget());

        itineraryInputService.saveStep7(inputId, request);

        return ResponseEntity.ok("Step 7 저장 완료");
    }

    // ============================================================
    // Step 8: 숙소
    // ============================================================

    @PutMapping("/{inputId}/step8")
    public ResponseEntity<String> saveStep8(
            @PathVariable Integer inputId,  // Long → Integer
            @RequestBody @Valid Step8RequestDto request) {

        log.info("Step 8 API 호출: inputId={}, 숙소={}",
                inputId, request.getNeedsAccommodation());

        itineraryInputService.saveStep8(inputId, request);

        return ResponseEntity.ok("✅ 8단계 입력 완료! 일정 생성이 가능합니다.");
    }

    // ============================================================
    // 조회 API
    // ============================================================

    @GetMapping("/{inputId}")
    public ResponseEntity<ItineraryInputResponseDto> getInput(
            @PathVariable Integer inputId) {  // Long → Integer

        log.info("입력 조회 API 호출: inputId={}", inputId);

        ItineraryInputResponseDto response = itineraryInputService.getInput(inputId);

        return ResponseEntity.ok(response);
    }
}