package com.example.WayGo.Constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * API 에러 코드 정의
 *
 * 코드 체계:
 * TR0xx: 입력 검증 에러
 * TR1xx: 언어 관련 에러
 * TR2xx: API 관련 에러
 * TR3xx: 파일 관련 에러
 * TR4xx: 네트워크 에러
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 입력 검증 에러 (TR0xx)
    EMPTY_TEXT("TR001", "번역할 텍스트를 입력해주세요"),
    TEXT_TOO_LONG("TR002", "텍스트는 5000자 이하여야 합니다"),
    INVALID_REQUEST("TR003", "잘못된 요청입니다"),

    // 언어 관련 에러 (TR1xx)
    UNSUPPORTED_LANGUAGE("TR101", "지원하지 않는 언어입니다"),
    INVALID_LANGUAGE_CODE("TR102", "잘못된 언어 코드입니다"),
    SAME_LANGUAGE("TR103", "원본 언어와 목표 언어가 같습니다"),
    LANGUAGE_DETECTION_FAILED("TR104", "언어 감지에 실패했습니다"),

    // API 관련 에러 (TR2xx)
    API_LIMIT_EXCEEDED("TR201", "일일 번역 한도를 초과했습니다"),
    API_KEY_INVALID("TR202", "API 키가 유효하지 않습니다"),
    API_TIMEOUT("TR203", "번역 시간이 초과되었습니다"),
    TRANSLATION_FAILED("TR204", "번역에 실패했습니다"),

    // 파일 관련 에러 (TR3xx)
    FILE_TOO_LARGE("TR301", "파일 크기는 10MB 이하여야 합니다"),
    INVALID_FILE_FORMAT("TR302", "지원하지 않는 파일 형식입니다"),
    FILE_UPLOAD_FAILED("TR303", "파일 업로드에 실패했습니다"),
    FILE_READ_FAILED("TR304", "파일을 읽을 수 없습니다"),

    // 네트워크 에러 (TR4xx)
    NETWORK_ERROR("TR401", "네트워크 연결을 확인해주세요"),
    SERVER_ERROR("TR402", "서버 오류가 발생했습니다"),
    SERVICE_UNAVAILABLE("TR403", "서비스를 일시적으로 사용할 수 없습니다"),

    // 기타
    UNKNOWN_ERROR("TR999", "알 수 없는 오류가 발생했습니다");

    private final String code;      // 에러 코드 (예: TR001)
    private final String message;   // 에러 메시지
}