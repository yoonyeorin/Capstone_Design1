package com.example.WayGo.Service;

import com.example.WayGo.Dto.TrendingCityDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmadeusService {

    private final RestTemplate restTemplate;

    @Value("${amadeus.api.key}")
    private String apiKey;

    @Value("${amadeus.api.secret}")
    private String apiSecret;

    @Value("${amadeus.api.base-url}")
    private String baseUrl;

    // Access Token 캐싱
    private String accessToken;
    private LocalDateTime tokenExpiryTime;

    /**
     * Access Token 발급
     */
    private void getAccessToken() {
        // 토큰이 유효하면 재사용
        if (accessToken != null && LocalDateTime.now().isBefore(tokenExpiryTime)) {
            return;
        }

        String url = baseUrl + "/v1/security/oauth2/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", apiKey);
        body.add("client_secret", apiSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("access_token")) {
                this.accessToken = (String) responseBody.get("access_token");
                // 토큰 유효기간 설정 (보통 30분, 여유있게 25분으로 설정)
                this.tokenExpiryTime = LocalDateTime.now().plusMinutes(25);
                log.info("Amadeus Access Token 발급 완료");
            }
        } catch (Exception e) {
            log.error("Amadeus Access Token 발급 실패: {}", e.getMessage());
            throw new RuntimeException("Amadeus API 인증 실패");
        }
    }

    /**
     * 실시간 인기 여행지 Top 10 조회 (Flight Inspiration Search 사용)
     */
    public List<TrendingCityDTO> getTrendingDestinations() {
        getAccessToken(); // 토큰 확인/발급

        // Flight Inspiration Search API 사용 (Test 환경에서 데이터 제공)
        String originCode = "PAR"; // 파리 (Test API에서 잘 작동)

        String url = String.format("%s/v1/shopping/flight-destinations?origin=%s&maxPrice=500",
                baseUrl, originCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<?> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            log.info("Amadeus API 응답: {}", responseBody);

            if (responseBody != null && responseBody.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");

                if (data == null || data.isEmpty()) {
                    log.warn("Amadeus API 응답 데이터가 비어있습니다. URL: {}", url);
                }

                // dictionaries에서 도시 정보 추출
                Map<String, Object> dictionaries = (Map<String, Object>) responseBody.get("dictionaries");
                Map<String, Object> locations = dictionaries != null ?
                        (Map<String, Object>) dictionaries.get("locations") : new HashMap<>();

                return data.stream()
                        .limit(10)
                        .map(item -> convertFlightDestinationToDTO(item, locations))
                        .collect(Collectors.toList());
            }

            log.warn("Amadeus API 응답에 data 필드가 없습니다. responseBody: {}", responseBody);
            throw new RuntimeException("Amadeus API 응답 데이터 없음");

        } catch (Exception e) {
            log.error("Amadeus API 호출 실패: URL={}, Error={}", url, e.getMessage(), e);
            throw new RuntimeException("Amadeus API 호출 실패: " + e.getMessage());
        }
    }

    /**
     * Flight Destination API 응답을 DTO로 변환
     */
    private TrendingCityDTO convertFlightDestinationToDTO(Map<String, Object> data, Map<String, Object> locations) {
        String cityCode = (String) data.get("destination");

        // 1단계: IATA 코드 → 영문 도시명
        String englishCityName = cityCode; // 기본값
        if (locations != null && locations.containsKey(cityCode)) {
            Map<String, Object> locationInfo = (Map<String, Object>) locations.get(cityCode);
            String detailedName = (String) locationInfo.get("detailedName");
            if (detailedName != null) {
                // "ADOLFO SUAREZ BARAJAS" → "Madrid" 형태로 변환
                englishCityName = extractCityName(detailedName, cityCode);
            }
        }

        // 2단계: 수동 매핑 확인 (가장 빠름)
        Map<String, String[]> cityMapping = getCityMapping();
        String[] cityInfo;

        if (cityMapping.containsKey(cityCode)) {
            cityInfo = cityMapping.get(cityCode);
        } else {
            // 3단계: 팀원1의 번역 기능 사용 (옵션)
            // String koreanName = translationService.translateToKorean(englishCityName);
            cityInfo = new String[]{englishCityName, "Unknown"};
        }

        // 가격 정보로 인기도 점수 계산 (가격이 낮을수록 인기)
        Integer travelScore = 80; // 기본 점수
        try {
            Map<String, Object> price = (Map<String, Object>) data.get("price");
            if (price != null && price.containsKey("total")) {
                String totalStr = String.valueOf(price.get("total"));
                Double total = Double.parseDouble(totalStr);
                travelScore = Math.max(100 - (total.intValue() / 10), 50); // 간단한 점수 계산
            }
        } catch (Exception e) {
            log.warn("가격 정보 파싱 실패: {}", e.getMessage());
        }

        return TrendingCityDTO.builder()
                .cityCode(cityCode)
                .cityName(cityInfo[0])
                .countryName(cityInfo[1])
                .travelScore(travelScore)
                .build();
    }

    /**
     * 공항명에서 도시명 추출
     */
    private String extractCityName(String detailedName, String cityCode) {
        // "ADOLFO SUAREZ BARAJAS" 같은 공항명에서 도시명 추출
        // 간단하게 첫 단어만 사용 (더 정교한 로직 필요 시 개선 가능)

        // 공통 공항 키워드 제거
        detailedName = detailedName
                .replace("INTL", "")
                .replace("INTERNATIONAL", "")
                .replace("AIRPORT", "")
                .replace("RAIL STN", "")
                .replace("RAILWAY STN", "")
                .trim();

        // 첫 단어 반환
        String[] words = detailedName.split(" ");
        return words.length > 0 ? words[0] : cityCode;
    }

    /**
     * IATA 도시 코드 -> 한글 도시명 매핑
     */
    private Map<String, String[]> getCityMapping() {
        Map<String, String[]> mapping = new HashMap<>();
        // [한글 도시명, 국가명]
        mapping.put("TYO", new String[]{"도쿄", "일본"});
        mapping.put("NRT", new String[]{"도쿄", "일본"});
        mapping.put("PAR", new String[]{"파리", "프랑스"});
        mapping.put("NYC", new String[]{"뉴욕", "미국"});
        mapping.put("BKK", new String[]{"방콕", "태국"});
        mapping.put("LON", new String[]{"런던", "영국"});
        mapping.put("OSA", new String[]{"오사카", "일본"});
        mapping.put("SIN", new String[]{"싱가포르", "싱가포르"});
        mapping.put("BCN", new String[]{"바르셀로나", "스페인"});
        mapping.put("ROM", new String[]{"로마", "이탈리아"});
        mapping.put("DXB", new String[]{"두바이", "UAE"});
        mapping.put("HKG", new String[]{"홍콩", "중국"});
        mapping.put("TPE", new String[]{"타이페이", "대만"});
        mapping.put("SYD", new String[]{"시드니", "호주"});
        mapping.put("LAX", new String[]{"로스앤젤레스", "미국"});
        mapping.put("SEL", new String[]{"서울", "한국"});
        mapping.put("SHA", new String[]{"상하이", "중국"});
        mapping.put("BER", new String[]{"베를린", "독일"});
        mapping.put("IST", new String[]{"이스탄불", "터키"});
        mapping.put("DPS", new String[]{"발리", "인도네시아"});
        mapping.put("MAD", new String[]{"마드리드", "스페인"});
        mapping.put("FRA", new String[]{"프랑크푸르트", "독일"});
        mapping.put("AMS", new String[]{"암스테르담", "네덜란드"});
        mapping.put("MUC", new String[]{"뮌헨", "독일"});
        mapping.put("FCO", new String[]{"로마", "이탈리아"});
        mapping.put("VIE", new String[]{"빈", "오스트리아"});
        mapping.put("ZRH", new String[]{"취리히", "스위스"});
        mapping.put("CPH", new String[]{"코펜하겐", "덴마크"});
        mapping.put("LIS", new String[]{"리스본", "포르투갈"});
        mapping.put("MIA", new String[]{"마이애미", "미국"});

        // Amadeus API에서 자주 나오는 추가 도시들
        mapping.put("TUN", new String[]{"튀니스", "튀니지"});
        mapping.put("OPO", new String[]{"포르투", "포르투갈"});
        mapping.put("LIN", new String[]{"밀라노", "이탈리아"});
        mapping.put("ATH", new String[]{"아테네", "그리스"});
        mapping.put("RAK", new String[]{"마라케시", "모로코"});
        mapping.put("SAW", new String[]{"이스탄불", "터키"});
        mapping.put("SFO", new String[]{"샌프란시스코", "미국"});
        mapping.put("BOS", new String[]{"보스턴", "미국"});
        mapping.put("PVG", new String[]{"상하이", "중국"});
        mapping.put("XMN", new String[]{"샤먼", "중국"});
        mapping.put("MCT", new String[]{"무스카트", "오만"});
        mapping.put("FDF", new String[]{"포르드프랑스", "마르티니크"});
        mapping.put("PTP", new String[]{"푸앵타피트르", "과들루프"});
        mapping.put("CDG", new String[]{"파리", "프랑스"});
        mapping.put("ORY", new String[]{"파리", "프랑스"});

        // 기차역 코드 (XPG, QQS, XYD 등)
        mapping.put("XPG", new String[]{"파리 북역", "프랑스"});
        mapping.put("QQS", new String[]{"런던 세인트판크라스", "영국"});
        mapping.put("XYD", new String[]{"리옹 파르디외", "프랑스"});

        return mapping;
    }
}