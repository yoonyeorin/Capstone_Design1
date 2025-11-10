package com.example.WayGo.Service.Itinerary;

import com.example.WayGo.Dto.Itinerary.*;
import com.example.WayGo.Entity.ItineraryInput;
import com.example.WayGo.Entity.UserEntity;
import com.example.WayGo.Entity.enums.*;
import com.example.WayGo.Exception.DateOverlapException;
import com.example.WayGo.Exception.InputNotFoundException;
import com.example.WayGo.Repository.Itinerary.ItineraryInputRepository;
import com.example.WayGo.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 일정 입력 처리 서비스
 *
 * ⚠️ ID 타입: Integer
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ItineraryInputService {

    private final ItineraryInputRepository inputRepository;
    private final UserRepository userRepository;
    private final GooglePlacesService googlePlacesService;

    // ============================================================
    // Step 1: 도시 검색 & 선택
    // ============================================================

    public List<Step1ResponseDto> searchCity(Step1RequestDto request) {
        log.info("도시 검색: {}", request.getCityName());
        return googlePlacesService.searchCity(request.getCityName());
    }

    @Transactional
    public Integer saveStep1(String loginId, Step1ResponseDto cityInfo) {  // Long → Integer
        log.info("Step 1 저장 시작: 사용자={}, 도시={}", loginId, cityInfo.getCityName());

        UserEntity user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        ItineraryInput input = new ItineraryInput();
        input.setUser(user);
        input.setDestinationCity(cityInfo.getCityName());
        input.setDestinationPlaceId(cityInfo.getPlaceId());
        input.setStatus(InputStatus.IN_PROGRESS);

        ItineraryInput saved = inputRepository.save(input);

        log.info("Step 1 저장 완료: inputId={}", saved.getId());
        return saved.getId();
    }

    // ============================================================
    // Step 2: 날짜 선택
    // ============================================================

    @Transactional
    public void saveStep2(Integer inputId, Step2RequestDto request) {  // Long → Integer
        log.info("Step 2 저장 시작: inputId={}", inputId);

        ItineraryInput input = inputRepository.findById(inputId)
                .orElseThrow(() -> new InputNotFoundException("입력을 찾을 수 없습니다"));

        boolean hasOverlap = inputRepository.existsByUserAndDateRangeOverlapExcluding(
                input.getUser(),
                request.getStartDate(),
                request.getEndDate(),
                inputId
        );

        if (hasOverlap) {
            log.warn("날짜 겹침 발견: {}-{}", request.getStartDate(), request.getEndDate());
            throw new DateOverlapException(
                    "해당 기간에 이미 일정이 있습니다. 다른 날짜를 선택해주세요."
            );
        }

        input.setStartDate(request.getStartDate());
        input.setEndDate(request.getEndDate());

        long days = ChronoUnit.DAYS.between(
                request.getStartDate(),
                request.getEndDate()
        ) + 1;
        input.setTotalDays((int) days);

        log.info("Step 2 저장 완료: {}일 여행", days);
    }

    // ============================================================
    // Step 3: 교통편
    // ============================================================

    @Transactional
    public List<FlightRecommendationDto> saveStep3(Integer inputId, Step3RequestDto request) {  // Long → Integer
        log.info("Step 3 저장 시작: inputId={}, hasTicket={}", inputId, request.getHasTicket());

        ItineraryInput input = inputRepository.findById(inputId)
                .orElseThrow(() -> new InputNotFoundException("입력을 찾을 수 없습니다"));

        input.setHasTransportTicket(request.getHasTicket());

        if (Boolean.TRUE.equals(request.getHasTicket())) {
            input.setArrivalTime(request.getArrivalTime());
            input.setDepartureTime(request.getDepartureTime());
            log.info("티켓 있음: 도착={}, 출발={}",
                    request.getArrivalTime(), request.getDepartureTime());
            return null;
        }

        log.info("티켓 없음: 비행기표 추천 시작");
        return recommendFlights(input);
    }

    private List<FlightRecommendationDto> recommendFlights(ItineraryInput input) {
        return List.of(
                FlightRecommendationDto.builder()
                        .flightNumber("KE123")
                        .airline("대한항공")
                        .price(350000)
                        .currency("KRW")
                        .build(),
                FlightRecommendationDto.builder()
                        .flightNumber("OZ456")
                        .airline("아시아나항공")
                        .price(330000)
                        .currency("KRW")
                        .build(),
                FlightRecommendationDto.builder()
                        .flightNumber("LJ789")
                        .airline("진에어")
                        .price(280000)
                        .currency("KRW")
                        .build()
        );
    }

    // ============================================================
    // Step 4: 인원수
    // ============================================================

    @Transactional
    public void saveStep4(Integer inputId, Step4RequestDto request) {  // Long → Integer
        log.info("Step 4 저장: inputId={}, 인원={}", inputId, request.getNumberOfPeople());

        ItineraryInput input = inputRepository.findById(inputId)
                .orElseThrow(() -> new InputNotFoundException("입력을 찾을 수 없습니다"));

        input.setNumberOfPeople(request.getNumberOfPeople());
    }

    // ============================================================
    // Step 5: 이동수단
    // ============================================================

    @Transactional
    public void saveStep5(Integer inputId, Step5RequestDto request) {  // Long → Integer
        log.info("Step 5 저장: inputId={}, 교통수단={}", inputId, request.getTransportTypes());

        ItineraryInput input = inputRepository.findById(inputId)
                .orElseThrow(() -> new InputNotFoundException("입력을 찾을 수 없습니다"));

        List<TransportType> transportTypes = request.getTransportTypes().stream()
                .map(str -> TransportType.valueOf(str.toUpperCase()))
                .collect(Collectors.toList());

        input.setTransportTypes(transportTypes);

        log.debug("변환된 Enum: {}", transportTypes);
    }

    // ============================================================
    // Step 6: 여행 취향 + 일정 밀도
    // ============================================================

    @Transactional
    public void saveStep6(Integer inputId, Step6RequestDto request) {  // Long → Integer
        log.info("Step 6 저장: inputId={}, 취향={}, 밀도={}",
                inputId, request.getTravelStyles(), request.getScheduleDensity());

        ItineraryInput input = inputRepository.findById(inputId)
                .orElseThrow(() -> new InputNotFoundException("입력을 찾을 수 없습니다"));

        List<TravelStyle> travelStyles = request.getTravelStyles().stream()
                .map(str -> TravelStyle.valueOf(str.toUpperCase()))
                .collect(Collectors.toList());

        input.setTravelStyles(travelStyles);

        ScheduleDensity density = ScheduleDensity.valueOf(
                request.getScheduleDensity().toUpperCase()
        );

        input.setScheduleDensity(density);
    }

    // ============================================================
    // Step 7: 예산
    // ============================================================

    @Transactional
    public void saveStep7(Integer inputId, Step7RequestDto request) {  // Long → Integer
        log.info("Step 7 저장: inputId={}, 예산={}", inputId, request.getBudget());

        ItineraryInput input = inputRepository.findById(inputId)
                .orElseThrow(() -> new InputNotFoundException("입력을 찾을 수 없습니다"));

        input.setBudget(request.getBudget());
    }

    // ============================================================
    // Step 8: 숙소
    // ============================================================

    @Transactional
    public void saveStep8(Integer inputId, Step8RequestDto request) {  // Long → Integer
        log.info("Step 8 저장: inputId={}, 숙소 필요={}",
                inputId, request.getNeedsAccommodation());

        ItineraryInput input = inputRepository.findById(inputId)
                .orElseThrow(() -> new InputNotFoundException("입력을 찾을 수 없습니다"));

        input.setNeedsAccommodation(request.getNeedsAccommodation());

        if (Boolean.TRUE.equals(request.getNeedsAccommodation())) {
            input.setAccommodationBudget(request.getAccommodationBudget());
        }

        input.setStatus(InputStatus.COMPLETED);

        log.info("✅ 8단계 입력 완료! 일정 생성 가능 상태");
    }

    // ============================================================
    // 조회 메서드
    // ============================================================

    public ItineraryInputResponseDto getInput(Integer inputId) {  // Long → Integer
        log.info("입력 조회: inputId={}", inputId);

        ItineraryInput input = inputRepository.findById(inputId)
                .orElseThrow(() -> new InputNotFoundException("입력을 찾을 수 없습니다"));

        return convertToResponseDto(input);
    }

    private ItineraryInputResponseDto convertToResponseDto(ItineraryInput input) {
        return ItineraryInputResponseDto.builder()
                .inputId(input.getId())
                .userId(input.getUser().getId())
                .status(input.getStatus().name())
                .destinationCity(input.getDestinationCity())
                .destinationPlaceId(input.getDestinationPlaceId())
                .startDate(input.getStartDate())
                .endDate(input.getEndDate())
                .totalDays(input.getTotalDays())
                .hasTransportTicket(input.getHasTransportTicket())
                .arrivalTime(input.getArrivalTime())
                .departureTime(input.getDepartureTime())
                .numberOfPeople(input.getNumberOfPeople())
                .transportTypes(
                        input.getTransportTypes().stream()
                                .map(Enum::name)
                                .collect(Collectors.toList())
                )
                .travelStyles(
                        input.getTravelStyles().stream()
                                .map(Enum::name)
                                .collect(Collectors.toList())
                )
                .scheduleDensity(input.getScheduleDensity().name())
                .budget(input.getBudget())
                .needsAccommodation(input.getNeedsAccommodation())
                .accommodationBudget(input.getAccommodationBudget())
                .createdAt(input.getCreatedAt())
                .updatedAt(input.getUpdatedAt())
                .build();
    }
}