package com.example.WayGo.Constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * API 에러 코드 정의
 * 코드 체계:
 * TR0xx: 번역/공통 입력 검증 에러
 * TR1xx: 언어 관련 에러
 * TR2xx: 번역 API 관련 에러
 * TR3xx: 파일 관련 에러
 * TR4xx: 네트워크 에러
 * IT0xx: 일정 입력 검증 에러
 * IT1xx: 일정 외부 API 관련 에러
 * IT2xx: 일정 도메인/데이터 관련 에러
 * U0xx : 사용자 관련 에러
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ===== 번역 / 공통 입력 검증 에러 (TR0xx) =====
    EMPTY_TEXT("TR001", "번역할 텍스트를 입력해주세요"),
    TEXT_TOO_LONG("TR002", "텍스트는 5000자 이하여야 합니다"),
    INVALID_REQUEST("TR003", "잘못된 요청입니다"),

    // ===== 언어 관련 에러 (TR1xx) =====
    UNSUPPORTED_LANGUAGE("TR101", "지원하지 않는 언어입니다"),
    INVALID_LANGUAGE_CODE("TR102", "잘못된 언어 코드입니다"),
    SAME_LANGUAGE("TR103", "원본 언어와 목표 언어가 같습니다"),
    LANGUAGE_DETECTION_FAILED("TR104", "언어 감지에 실패했습니다"),

    // ===== 번역 API 관련 에러 (TR2xx) =====
    API_LIMIT_EXCEEDED("TR201", "일일 번역 한도를 초과했습니다"),
    API_KEY_INVALID("TR202", "API 키가 유효하지 않습니다"),
    API_TIMEOUT("TR203", "번역 시간이 초과되었습니다"),
    TRANSLATION_FAILED("TR204", "번역에 실패했습니다"),

    // ===== 파일 관련 에러 (TR3xx) =====
    FILE_TOO_LARGE("TR301", "파일 크기는 10MB 이하여야 합니다"),
    INVALID_FILE_FORMAT("TR302", "지원하지 않는 파일 형식입니다"),
    FILE_UPLOAD_FAILED("TR303", "파일 업로드에 실패했습니다"),
    FILE_READ_FAILED("TR304", "파일을 읽을 수 없습니다"),

    // ===== 네트워크 에러 (TR4xx) =====
    NETWORK_ERROR("TR401", "네트워크 연결을 확인해주세요"),
    SERVER_ERROR("TR402", "서버 오류가 발생했습니다"),
    SERVICE_UNAVAILABLE("TR403", "서비스를 일시적으로 사용할 수 없습니다"),

    // ===== 일정 입력 검증 에러 (IT0xx) =====
    EMPTY_CITY("IT001", "도시를 입력해주세요"),
    INVALID_DATE_RANGE("IT002", "여행 종료일은 시작일 이후여야 합니다"),
    DATE_TOO_FAR("IT003", "여행 날짜는 2년 이내로 설정해주세요"),
    INVALID_BUDGET("IT004", "예산은 0 이상이어야 합니다"),

    // ===== 일정 외부 API 에러 (IT1xx) =====
    GOOGLE_PLACES_API_FAILED("IT101", "장소 검색에 실패했습니다"),
    GOOGLE_GEOCODING_API_FAILED("IT102", "도시 좌표 조회에 실패했습니다"),
    GOOGLE_DISTANCE_API_FAILED("IT103", "거리 계산에 실패했습니다"),
    WEATHER_API_FAILED("IT104", "날씨 정보 조회에 실패했습니다"),
    FLIGHT_API_FAILED("IT105", "항공권 정보 조회에 실패했습니다"),
    HOTEL_API_FAILED("IT106", "숙소 정보 조회에 실패했습니다"),

    // ===== 일정 도메인/데이터 에러 (IT2xx) =====
    ITINERARY_NOT_FOUND("IT201", "일정을 찾을 수 없습니다"),
    NO_PLACES_FOUND("IT202", "추천 장소를 찾을 수 없습니다"),
    CITY_NOT_FOUND("IT203", "해당 도시를 찾을 수 없습니다"),
    DATE_OVERLAP("IT204", "이미 등록된 여행 일정과 날짜가 겹칩니다"),

    // ===== 사용자 / 권한 에러 (U0xx / IT2xx) =====
    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다."),
    ITINERARY_FORBIDDEN("IT205", "해당 일정에 접근 권한이 없습니다."),

    // ===== 기타 =====
    UNKNOWN_ERROR("TR999", "알 수 없는 오류가 발생했습니다");

    private final String code;
    private final String message;
}
