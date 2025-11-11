package com.example.WayGo.Controller.Itinerary;

import com.example.WayGo.Dto.Itinerary.ItineraryGenerationRequestDto;
import com.example.WayGo.Dto.Itinerary.ItineraryResponseDto;
import com.example.WayGo.Service.Itinerary.ItineraryGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 일정 생성 및 조회 컨트롤러
 *
 * 8단계 입력을 기반으로 실제 여행 일정 생성
 */
@RestController
@RequestMapping("/api/itinerary")
@RequiredArgsConstructor
@Slf4j
public class ItineraryController {

    private final ItineraryGenerationService itineraryGenerationService;

    // ============================================================
    // 일정 생성
    // ============================================================

    /**
     * 일정 생성 API
     *
     * POST /api/itinerary/generate
     *
     * 요청:
     * {
     *   "inputId": 1
     * }
     *
     * 응답:
     * {
     *   "itineraryId": 1,
     *   "message": "일정이 생성되었습니다"
     * }
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateItinerary(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid ItineraryGenerationRequestDto request) {

        log.info("일정 생성 API 호출: 사용자={}, inputId={}",
                userDetails.getUsername(), request.getInputId());

        try {
            Long itineraryId = itineraryGenerationService.generateItinerary(request.getInputId());

            log.info("일정 생성 완료: itineraryId={}", itineraryId);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(new GenerationResponse(itineraryId, "일정이 생성되었습니다"));

        } catch (IllegalStateException e) {
            log.warn("일정 생성 실패: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("일정 생성 중 에러 발생", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("일정 생성 중 오류가 발생했습니다"));
        }
    }

    // ============================================================
    // 일정 조회
    // ============================================================

    /**
     * 일정 상세 조회 API
     *
     * GET /api/itinerary/{itineraryId}
     *
     * 응답:
     * {
     *   "itineraryId": 1,
     *   "title": "도쿄 2박 3일",
     *   "totalBudget": 500000,
     *   "totalSpent": 485000,
     *   "status": "ACTIVE",
     *   "days": [...]
     * }
     */
    @GetMapping("/{itineraryId}")
    public ResponseEntity<ItineraryResponseDto> getItinerary(
            @PathVariable Long itineraryId) {

        log.info("일정 조회 API 호출: itineraryId={}", itineraryId);

        ItineraryResponseDto response = itineraryGenerationService.getItinerary(itineraryId);

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // 응답 DTO (내부 클래스)
    // ============================================================

    /**
     * 일정 생성 성공 응답
     */
    private record GenerationResponse(Long itineraryId, String message) {
    }

    /**
     * 에러 응답
     */
    private record ErrorResponse(String message) {
    }
}