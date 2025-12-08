package com.example.WayGo.Controller.Itinerary;

import com.example.WayGo.Dto.Itinerary.*;
import com.example.WayGo.Service.Itinerary.CitySearchService;
import com.example.WayGo.Service.Itinerary.ItineraryGenerationService;
import com.example.WayGo.Service.Itinerary.ItineraryQueryService;
import com.example.WayGo.Service.Itinerary.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/itinerary")
@RequiredArgsConstructor
@Tag(name = "Itinerary", description = "여행 일정 생성 API")
public class ItineraryController {

    private final ItineraryGenerationService itineraryGenerationService;
    private final ItineraryQueryService itineraryQueryService;
    private final CitySearchService citySearchService;
    private final ItineraryRequestMapper itineraryRequestMapper;

    // ----------------------------
    // 1) Step1: 도시 검색 API
    // ----------------------------
    @Operation(
            summary = "도시 검색",
            description = "사용자가 입력한 검색어(query)를 기반으로 추천 도시 목록을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "도시 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CitySearchDto.class)))
    })
    @GetMapping("/cities")
    public ResponseEntity<List<CitySearchDto>> searchCities(
            @Parameter(description = "검색어", example = "tokyo")
            @RequestParam String query
    ) {
        return ResponseEntity.ok(citySearchService.searchCityList(query));
    }

    // ----------------------------
    // 2) Step2: 날짜 겹침 체크
    // ----------------------------
    @Operation(
            summary = "날짜 겹침 체크",
            description = "해당 기간에 이미 등록된 다른 여행 일정이 있는지 확인합니다. 세션 로그인 필요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "겹침 여부 조회 성공"),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다.")
    })
    @GetMapping("/check-overlap")
    public ResponseEntity<?> checkOverlap(
            @Parameter(description = "여행 시작 날짜", example = "2025-11-01")
            @RequestParam String startDate,
            @Parameter(description = "여행 종료 날짜", example = "2025-11-03")
            @RequestParam String endDate,
            HttpSession session
    ) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        return ResponseEntity.ok(
                itineraryGenerationService.checkDateOverlap(userId, startDate, endDate)
        );
    }

    // ----------------------------
    // 3) Step3: 일정 생성 (임시 저장, status=GENERATED)
    // ----------------------------
    @Operation(
            summary = "여행 일정 생성",
            description = "사용자의 입력값을 기반으로 날씨/장소/항공/숙소 분석 후 AI로 일정 초안을 생성합니다. "
                    + "생성된 일정은 status=GENERATED 로 임시 저장됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일정 생성 성공"),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다.")
    })
    @PostMapping("/generate")
    public ResponseEntity<?> generate(
            @RequestBody ItineraryGenerationFormDto form,
            HttpSession session
    ) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        // 한글 폼 -> 내부 DTO 변환
        ItineraryGenerationRequestDto request =
                itineraryRequestMapper.toGenerationRequest(form);

        Long itineraryId = itineraryGenerationService.generateItinerary(request, userId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "itineraryId", itineraryId
        ));
    }

    // ----------------------------
    // 4) Step4: 최종 저장 (ACTIVE 확정)
    // ----------------------------
    @Operation(
            summary = "일정 확정 저장",
            description = "GENERATED 상태의 일정을 ACTIVE로 확정합니다. "
                    + "기존 일정과 날짜가 겹치면 자동으로 이전 ACTIVE 일정 삭제."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "확정 저장 성공"),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다.")
    })
    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(
            @Parameter(description = "확정할 일정 ID")
            @RequestParam Long itineraryId,
            HttpSession session
    ) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        Long id = itineraryGenerationService.confirmItinerary(userId, itineraryId);
        return ResponseEntity.ok(Map.of("success", true, "id", id));
    }

    // ----------------------------
    // 5) 일정 상세 조회
    // ----------------------------
    @Operation(
            summary = "일정 상세 조회",
            description = "일정 ID를 기반으로 Day/Activity 전체를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ItineraryResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "일정 없음")
    })
    @GetMapping("/{itineraryId}")
    public ResponseEntity<ItineraryResponseDto> getDetail(
            @Parameter(description = "조회할 일정 ID")
            @PathVariable Long itineraryId
    ) {
        return ResponseEntity.ok(itineraryQueryService.getItinerary(itineraryId));
    }

    // ----------------------------
    // 6) AI 원본 텍스트 조회 (드롭다운 구조로 파싱)
    // ----------------------------
    @Operation(
            summary = "AI 일정 원문 조회 (드롭다운용)",
            description = "OpenAI가 생성한 원본 TEXT를 dayHeader와 dayContent로 파싱해서 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ItineraryTextResponseDto.class)))
    })
    @GetMapping("/{itineraryId}/ai-formatted")
    public ResponseEntity<ItineraryTextResponseDto> getAiFormatted(
            @Parameter(description = "일정 ID")
            @PathVariable Long itineraryId
    ) {
        return ResponseEntity.ok(itineraryQueryService.getAiFormattedText(itineraryId));
    }

    // ----------------------------
    // 7) AI 원본 텍스트 조회 (RAW - 파싱 안 함)
    // ----------------------------
    @Operation(
            summary = "AI 일정 원문 조회 (RAW)",
            description = "OpenAI가 생성한 원본 TEXT를 그대로 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
    })
    @GetMapping("/{itineraryId}/ai-raw")
    public ResponseEntity<ItineraryAiRawResponseDto> getAiRaw(
            @Parameter(description = "일정 ID")
            @PathVariable Long itineraryId
    ) {
        return ResponseEntity.ok(itineraryQueryService.getAiRawText(itineraryId));
    }

    // ----------------------------
    // 8) 일자별 캘린더 요약
    // ----------------------------
    @Operation(
            summary = "캘린더 요약",
            description = "각 날짜별 대표 장소 2~4개만 요약해서 제공하는 API (캘린더 뷰용)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
    })
    @GetMapping("/{itineraryId}/calendar-summary")
    public ResponseEntity<List<ItineraryCalendarSummaryDto>> getCalendarSummary(
            @Parameter(description = "일정 ID")
            @PathVariable Long itineraryId
    ) {
        return ResponseEntity.ok(itineraryQueryService.getCalendarSummary(itineraryId));
    }

    // ----------------------------
    // 9) (옵션) 날씨 디버그용 API - 위/경도 기반
    // ----------------------------
    @RestController
    @RequiredArgsConstructor
    public static class DebugWeatherController {

        private final WeatherService weatherService;

        /**
         * 예시 호출:
         *   GET /api/itinerary/debug/weather?start=2025-12-06&end=2025-12-09&lat=35.6895&lng=139.6917
         */
        @GetMapping("/api/itinerary/debug/weather")
        public List<WeatherInfoDto> debugWeather(
                @RequestParam String start,
                @RequestParam String end,
                @RequestParam Double lat,
                @RequestParam Double lng
        ) {
            return weatherService.getWeather(start, end, lat, lng);
        }
    }
}
