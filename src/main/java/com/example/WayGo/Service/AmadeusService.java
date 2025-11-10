package com.example.WayGo.Service;

import com.example.WayGo.Dto.TrendingCityDTO;
import com.example.WayGo.Dto.Translation.TextTranslationRequest;
import com.example.WayGo.Dto.Translation.TranslationResponse;
import com.example.WayGo.Service.Translation.TranslateService;
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
    private final TranslateService translateService; // 번역 서비스 추가

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

                // ✅ 중복 도시 제거: cityCode 기준으로 고유한 도시만 선택
                // 같은 도시가 여러 번 나오면 가장 높은 점수(=가장 저렴한 가격)를 가진 항목만 유지
                Map<String, TrendingCityDTO> uniqueCities = new LinkedHashMap<>();

                data.stream()
                        .map(item -> convertFlightDestinationToDTO(item, locations))
                        .forEach(dto -> {
                            String cityCode = dto.getCityCode();
                            // 이미 존재하는 도시면 점수 비교 후 높은 것만 유지
                            if (!uniqueCities.containsKey(cityCode) ||
                                    uniqueCities.get(cityCode).getTravelScore() < dto.getTravelScore()) {
                                uniqueCities.put(cityCode, dto);
                            }
                        });

                // 점수 순으로 정렬 후 Top 10 반환
                return uniqueCities.values().stream()
                        .sorted((a, b) -> b.getTravelScore().compareTo(a.getTravelScore()))
                        .limit(10)
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

        // cityCode → [한글 도시명, 국가명] 변환
        // locations 정보를 함께 전달하여 풀네임 추출 가능하게 함
        String[] cityInfo = getCityFullName(cityCode, locations);

        // 가격 정보로 인기도 점수 계산
        Integer travelScore = 80;
        try {
            Map<String, Object> price = (Map<String, Object>) data.get("price");
            if (price != null && price.containsKey("total")) {
                String totalStr = String.valueOf(price.get("total"));
                Double total = Double.parseDouble(totalStr);
                travelScore = Math.max(100 - (total.intValue() / 10), 50);
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
     * IATA 도시 코드를 한글 도시명과 국가명으로 변환
     * @param cityCode IATA 코드
     * @param locations Amadeus API의 locations 딕셔너리
     * @return [한글 도시명, 국가명] 배열
     */
    private String[] getCityFullName(String cityCode, Map<String, Object> locations) {
        Map<String, String[]> cityMapping = getCityMapping();

        // 1단계: 수동 매핑에 있으면 바로 반환
        if (cityMapping.containsKey(cityCode)) {
            log.debug("수동 매핑 사용: {} -> {}", cityCode, cityMapping.get(cityCode)[0]);
            return cityMapping.get(cityCode);
        }

        // 2단계: Amadeus API의 locations에서 영문 도시명 + 국가 코드 추출
        String englishCityName = cityCode;
        String countryCode = "Unknown";

        if (locations != null && locations.containsKey(cityCode)) {
            Map<String, Object> locationInfo = (Map<String, Object>) locations.get(cityCode);

            // countryCode 추출
            if (locationInfo.containsKey("countryCode")) {
                countryCode = (String) locationInfo.get("countryCode");
                log.debug("locations에서 국가 코드 추출: {} -> {}", cityCode, countryCode);
            }

            String detailedName = (String) locationInfo.get("detailedName");
            if (detailedName != null) {
                // 공항명 → 도시명 변환 (매핑 테이블 사용)
                englishCityName = extractCityName(detailedName, cityCode);
                log.debug("API에서 도시명 추출: {} -> {}", cityCode, englishCityName);
            }
        } else {
            // locations에 정보가 없는 경우
            log.warn("locations에 {} 정보 없음", cityCode);
        }

        // ✅ countryCode가 Unknown이면 cityCode로 국가 추론
        if (countryCode.equals("Unknown")) {
            countryCode = inferCountryFromCityCode(cityCode);
            log.info("cityCode로 국가 추론: {} -> {}", cityCode, countryCode);
        }

        // 3단계: Google Translation API로 번역 + 국가명 변환
        try {
            String koreanCityName = translateToKorean(englishCityName);
            String koreanCountryName = translateCountryCode(countryCode);

            log.info("번역 완료: {} -> {}, {} -> {}",
                    englishCityName, koreanCityName, countryCode, koreanCountryName);

            return new String[]{koreanCityName, koreanCountryName};

        } catch (Exception e) {
            log.warn("번역 실패 ({}): {}. 영문 그대로 사용", englishCityName, e.getMessage());
            return new String[]{englishCityName, translateCountryCode(countryCode)};
        }
    }
    /**
     * 공항/도시 코드로 국가 코드 추론
     * locations에 정보가 없거나 countryCode가 없을 때 사용
     */
    private String inferCountryFromCityCode(String cityCode) {
        Map<String, String> cityToCountry = new HashMap<>();

        // ✅ 주요 공항/도시 → 국가 코드 매핑
        cityToCountry.put("MAD", "ES");  // 마드리드 → 스페인
        cityToCountry.put("BCN", "ES");  // 바르셀로나 → 스페인
        cityToCountry.put("FCO", "IT");  // 로마 피우미치노 → 이탈리아
        cityToCountry.put("LIN", "IT");  // 밀라노 리나테 → 이탈리아
        cityToCountry.put("CDG", "FR");  // 파리 샤를드골 → 프랑스
        cityToCountry.put("ORY", "FR");  // 파리 오를리 → 프랑스
        cityToCountry.put("XPG", "FR");  // 파리 북역 → 프랑스
        cityToCountry.put("XYD", "FR");  // 리옹 → 프랑스
        cityToCountry.put("LIS", "PT");  // 리스본 → 포르투갈
        cityToCountry.put("OPO", "PT");  // 포르투 → 포르투갈
        cityToCountry.put("FRA", "DE");  // 프랑크푸르트 → 독일
        cityToCountry.put("TUN", "TN");  // 튀니스 → 튀니지
        cityToCountry.put("RAK", "MA");  // 마라케시 → 모로코
        cityToCountry.put("QQS", "GB");  // 런던 세인트판크라스 → 영국
        cityToCountry.put("SAW", "TR");  // 이스탄불 → 터키
        cityToCountry.put("BOS", "US");  // 보스턴 → 미국
        cityToCountry.put("CPH", "DK");  // 코펜하겐 → 덴마크
        cityToCountry.put("PVG", "CN");  // 상하이 푸둥 → 중국
        cityToCountry.put("XMN", "CN");  // 샤먼 → 중국
        cityToCountry.put("FDF", "MQ");  // 포르드프랑스 → 마르티니크
        cityToCountry.put("PTP", "GP");  // 푸앵타피트르 → 과들루프
        cityToCountry.put("TFU", "CN");  // 청두 → 중국

        // 추가 주요 도시들
        cityToCountry.put("NRT", "JP");  // 도쿄 나리타
        cityToCountry.put("HND", "JP");  // 도쿄 하네다
        cityToCountry.put("ICN", "KR");  // 서울 인천
        cityToCountry.put("GMP", "KR");  // 서울 김포
        cityToCountry.put("BKK", "TH");  // 방콕
        cityToCountry.put("SIN", "SG");  // 싱가포르
        cityToCountry.put("DXB", "AE");  // 두바이
        cityToCountry.put("SYD", "AU");  // 시드니
        cityToCountry.put("AMS", "NL");  // 암스테르담
        cityToCountry.put("LHR", "GB");  // 런던 히드로
        cityToCountry.put("JFK", "US");  // 뉴욕 JFK
        cityToCountry.put("LAX", "US");  // 로스앤젤레스

        return cityToCountry.getOrDefault(cityCode, "Unknown");
    }

    /**
     * 영문 텍스트를 한글로 번역
     */
    private String translateToKorean(String englishText) {
        TextTranslationRequest request = new TextTranslationRequest();
        request.setText(englishText);
        request.setSourceLanguage("en");
        request.setTargetLanguage("ko");

        TranslationResponse response = translateService.translateText(request);
        return response.getTranslatedText();
    }

    /**
     * ISO 국가 코드를 한글 국가명으로 변환
     * @param countryCode ISO 2자리 국가 코드 (예: ES, GB, IT)
     * @return 한글 국가명
     */
    private String translateCountryCode(String countryCode) {
        if (countryCode == null || countryCode.equals("Unknown")) {
            return "Unknown";
        }

        // 1단계: 수동 매핑 확인 (가장 빠름)
        Map<String, String> countryMap = getCountryMapping();
        if (countryMap.containsKey(countryCode)) {
            log.debug("국가 매핑 사용: {} -> {}", countryCode, countryMap.get(countryCode));
            return countryMap.get(countryCode);
        }

        // 2단계: 매핑에 없으면 ISO 코드 → 영문 국가명 변환 후 번역
        try {
            String englishCountryName = getEnglishCountryName(countryCode);
            if (englishCountryName != null && !englishCountryName.equals(countryCode)) {
                String koreanCountryName = translateToKorean(englishCountryName);
                log.info("국가 번역 완료: {} ({}) -> {}", countryCode, englishCountryName, koreanCountryName);
                return koreanCountryName;
            }
        } catch (Exception e) {
            log.warn("국가 번역 실패: {}", countryCode);
        }

        return "Unknown";
    }

    /**
     * ISO 국가 코드 → 영문 국가명 변환
     */
    private String getEnglishCountryName(String countryCode) {
        // Java의 Locale 클래스 활용
        java.util.Locale locale = new java.util.Locale("", countryCode);
        String englishName = locale.getDisplayCountry(java.util.Locale.ENGLISH);

        // 유효한 국가명이면 반환
        if (englishName != null && !englishName.isEmpty() && !englishName.equals(countryCode)) {
            return englishName;
        }

        return countryCode;
    }

    /**
     * 자주 사용되는 국가 코드 매핑 (캐시 역할)
     */
    private Map<String, String> getCountryMapping() {
        Map<String, String> countryMap = new HashMap<>();

        // 로그에서 확인된 국가들
        countryMap.put("ES", "스페인");      // MAD, BCN
        countryMap.put("GB", "영국");        // QQS
        countryMap.put("TN", "튀니지");      // TUN
        countryMap.put("PT", "포르투갈");    // OPO, LIS
        countryMap.put("IT", "이탈리아");    // LIN, FCO
        countryMap.put("MA", "모로코");      // RAK
        countryMap.put("FR", "프랑스");      // CDG, ORY, XPG, XYD
        countryMap.put("DE", "독일");        // FRA
        countryMap.put("US", "미국");        // BOS
        countryMap.put("TR", "터키");        // SAW
        countryMap.put("CN", "중국");        // PVG, XMN
        countryMap.put("DK", "덴마크");      // CPH
        countryMap.put("MQ", "마르티니크");  // FDF
        countryMap.put("GP", "과들루프");    // PTP

        // 추가 주요 국가들
        countryMap.put("JP", "일본");
        countryMap.put("KR", "한국");
        countryMap.put("TH", "태국");
        countryMap.put("SG", "싱가포르");
        countryMap.put("AE", "UAE");
        countryMap.put("AU", "호주");
        countryMap.put("NL", "네덜란드");
        countryMap.put("CH", "스위스");
        countryMap.put("AT", "오스트리아");
        countryMap.put("GR", "그리스");
        countryMap.put("HK", "홍콩");
        countryMap.put("TW", "대만");
        countryMap.put("ID", "인도네시아");
        countryMap.put("OM", "오만");

        return countryMap;
    }

    /**
     * 공항명에서 도시명 추출 (개선 버전)
     * Amadeus locations의 detailedName을 파싱하여 실제 도시명 반환
     */
    private String extractCityName(String detailedName, String cityCode) {
        if (detailedName == null || detailedName.isEmpty()) {
            return cityCode;
        }

        // 1단계: 공항 관련 키워드 제거
        String cleaned = detailedName
                .replace("INTL", "")
                .replace("INTERNATIONAL", "")
                .replace("AIRPORT", "")
                .replace("RAIL STN", "")
                .replace("RAILWAY STN", "")
                .replace("RAILST", "")  // QQS (ST PANCRAS INTL RAILST)
                .trim();

        // 2단계: 공항명 → 도시명 매핑 테이블에서 확인
        String[] words = cleaned.split(" ");
        if (words.length > 0) {
            String airportName = words[0];

            // ✅ 공항명으로 도시 찾기
            Map<String, String> airportToCity = getAirportToCityMapping();
            if (airportToCity.containsKey(airportName)) {
                String cityName = airportToCity.get(airportName);
                log.debug("공항명 매핑 사용: {} ({}) -> {}", cityCode, airportName, cityName);
                return cityName;
            }

            // 매핑에 없으면 첫 단어 그대로 반환
            return airportName;
        }

        return cityCode;
    }

    /**
     * 공항명 → 영문 도시명 매핑 테이블
     * 로그에서 확인된 실제 Amadeus API 데이터 기반
     */
    private Map<String, String> getAirportToCityMapping() {
        Map<String, String> mapping = new HashMap<>();

        // ✅ 로그에서 확인된 실제 공항들
        mapping.put("FIUMICINO", "ROME");              // FCO → 로마
        mapping.put("ORLY", "PARIS");                  // ORY → 파리
        mapping.put("CHARLES", "PARIS");               // CDG → 파리 (CHARLES DE GAULLE)
        mapping.put("PUDONG", "SHANGHAI");             // PVG → 상하이
        mapping.put("GARE", "PARIS");                  // XPG → 파리 (GARE DU NORD)
        mapping.put("ST", "LONDON");                   // QQS → 런던 (ST PANCRAS)
        mapping.put("FRANCISCO", "PORTO");             // OPO → 포르투 (FRANCISCO SA CARNEIRO)
        mapping.put("LINATE", "MILAN");                // LIN → 밀라노
        mapping.put("ADOLFO", "MADRID");               // MAD → 마드리드 (ADOLFO SUAREZ BARAJAS)
        mapping.put("FRANKFURT", "FRANKFURT");         // FRA → 프랑크푸르트
        mapping.put("KASTRUP", "COPENHAGEN");          // CPH → 코펜하겐
        mapping.put("EDWARD", "BOSTON");               // BOS → 보스턴 (EDWARD L LOGAN)
        mapping.put("SABIHA", "ISTANBUL");             // SAW → 이스탄불 (SABIHA GOKCEN)
        mapping.put("MARTINIQUE", "FORT-DE-FRANCE");   // FDF → 포르드프랑스
        mapping.put("CARTHAGE", "TUNIS");              // TUN → 튀니스
        mapping.put("JOSEP", "BARCELONA");             // BCN → 바르셀로나 (JOSEP TARRADELLAS)
        mapping.put("LE", "POINTE-A-PITRE");          // PTP → 푸앵타피트르 (LE RAIZET)
        mapping.put("PART-DIEU", "LYON");             // XYD → 리옹 (PART-DIEU RAILWAY)
        mapping.put("TIANFU", "CHENGDU");             // TFU → 청두
        mapping.put("GAOQI", "XIAMEN");               // XMN → 샤먼
        mapping.put("MENARA", "MARRAKECH");           // RAK → 마라케시

        // 추가 주요 공항들
        mapping.put("NARITA", "TOKYO");
        mapping.put("HANEDA", "TOKYO");
        mapping.put("HEATHROW", "LONDON");
        mapping.put("GATWICK", "LONDON");
        mapping.put("KENNEDY", "NEW YORK");
        mapping.put("LAGUARDIA", "NEW YORK");
        mapping.put("NEWARK", "NEW YORK");
        mapping.put("BARAJAS", "MADRID");
        mapping.put("SCHIPHOL", "AMSTERDAM");
        mapping.put("CHANGI", "SINGAPORE");
        mapping.put("INCHEON", "SEOUL");
        mapping.put("GIMPO", "SEOUL");
        mapping.put("SUVARNABHUMI", "BANGKOK");
        mapping.put("HONGQIAO", "SHANGHAI");

        return mapping;
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