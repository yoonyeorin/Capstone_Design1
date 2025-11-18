package com.example.WayGo.Controller;

import com.example.WayGo.Dto.PlaceDTO;
import com.example.WayGo.Dto.PlacesSearchRequest;
import com.example.WayGo.Service.GoogleNearbyPlacesService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Places", description = "주변 장소 검색 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PlacesController {

    private final GoogleNearbyPlacesService googleNearbyPlacesService;

    /**
     * 주변 장소 검색 (화장실, 병원, 편의점, 주유소)
     */
    @Operation(summary = "주변 장소 검색",
            description = "현재 위치 기준 1km 반경 내의 화장실, 병원, 편의점, 주유소를 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/nearby")
    public ResponseEntity<Map<String, Object>> searchNearbyPlaces(@RequestBody PlacesSearchRequest request) {
        try {
            // 필수 파라미터 검증
            if (request.getLatitude() == null || request.getLongitude() == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "위도와 경도는 필수 입력값입니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // ⭐ 추가: 위도/경도 유효성 검증
            if (request.getLatitude() == 0.0 && request.getLongitude() == 0.0) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "올바른 위도와 경도를 입력해주세요.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (request.getCategory() == null || request.getCategory().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "카테고리는 필수 입력값입니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // ⭐ 추가: radius 기본값 설정
            if (request.getRadius() == null || request.getRadius() <= 0) {
                request.setRadius(1000); // 기본 1km
            }

            // 카테고리 유효성 검증
            if (!googleNearbyPlacesService.isValidCategory(request.getCategory())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "유효하지 않은 카테고리입니다. (restroom, hospital, convenience_store, gas_station 중 선택)");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            log.info("장소 검색 요청: lat={}, lng={}, category={}, radius={}m",
                    request.getLatitude(), request.getLongitude(), request.getCategory(), request.getRadius());

            // 장소 검색 실행
            List<PlaceDTO> places = googleNearbyPlacesService.searchNearbyPlaces(
                    request.getLatitude(),
                    request.getLongitude(),
                    request.getCategory(),
                    request.getRadius()
            );

            // 성공 응답
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "장소 검색이 완료되었습니다.");
            response.put("count", places.size());
            response.put("data", places);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("장소 검색 중 오류 발생: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "장소 검색 중 오류가 발생했습니다: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 지원하는 카테고리 목록 조회
     */
    @Operation(summary = "지원 카테고리 목록",
            description = "검색 가능한 카테고리 목록을 조회합니다.")
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getCategories() {
        Map<String, String> categories = new HashMap<>();
        categories.put("restroom", "화장실");
        categories.put("hospital", "병원");
        categories.put("convenience_store", "편의점");
        categories.put("gas_station", "주유소");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", categories);

        return ResponseEntity.ok(response);
    }
}