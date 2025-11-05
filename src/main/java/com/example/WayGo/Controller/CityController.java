package com.example.WayGo.Controller;

import com.example.WayGo.Dto.LandmarkDTO;
import com.example.WayGo.Dto.TrendingCityDTO;
import com.example.WayGo.Service.AmadeusService;
import com.example.WayGo.Service.LandmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "City", description = "도시 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cities")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CityController {

    private final AmadeusService amadeusService;
    private final LandmarkService landmarkService;

    /**
     * 실시간 인기 도시 Top 10 조회
     */
    @Operation(summary = "실시간 인기 도시 Top 10",
            description = "Amadeus API를 통해 실시간 인기 여행 도시 순위를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/trending")
    public ResponseEntity<Map<String, Object>> getTrendingCities() {
        try {
            List<TrendingCityDTO> trendingCities = amadeusService.getTrendingDestinations();

            // 순위 부여 (1~10)
            for (int i = 0; i < trendingCities.size(); i++) {
                trendingCities.get(i).setRank(i + 1);
            }

            log.info("실시간 인기 도시 조회 성공: {} 개", trendingCities.size());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "실시간 인기 도시를 성공적으로 조회했습니다.");
            response.put("data", trendingCities);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("실시간 인기 도시 조회 실패: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        } catch (Exception e) {
            log.error("실시간 인기 도시 조회 중 예상치 못한 오류: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "인기 도시 조회 중 오류가 발생했습니다.");

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 도시 검색 - 유명 랜드마크 Top 3 조회
     */
    @Operation(summary = "도시 검색 - 랜드마크 조회",
            description = "도시명으로 검색하여 해당 도시의 유명 랜드마크 Top 3를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchCity(
            @Parameter(description = "도시명 (한글 또는 영문)", example = "파리")
            @RequestParam String cityName) {

        try {
            if (cityName == null || cityName.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "도시명을 입력해주세요.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            log.info("도시 검색 요청: {}", cityName);

            List<LandmarkDTO> landmarks = landmarkService.getTopLandmarks(cityName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cityName", cityName);
            response.put("landmarks", landmarks);
            response.put("landmarkCount", landmarks.size());
            response.put("message", landmarks.isEmpty() ?
                    "검색 결과가 없습니다." : "랜드마크를 성공적으로 조회했습니다.");

            log.info("도시 검색 성공: {} - {} 개의 랜드마크", cityName, landmarks.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("도시 검색 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "도시 검색 중 오류가 발생했습니다.");

            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}