package com.example.WayGo.Entity.enums;

import lombok.Getter;

@Getter
public enum SupportedLanguage {

    KOREAN("한국어", "ko", "ko-KR"),
    ENGLISH("영어", "en", "en-US"),
    JAPANESE("일본어", "ja", "ja-JP"),
    CHINESE_SIMPLIFIED("중국어(간체)", "zh-CN", "zh-CN"),
    CHINESE_TRADITIONAL("중국어(번체)", "zh-TW", "zh-TW"),
    SPANISH("스페인어", "es", "es-ES"),
    FRENCH("프랑스어", "fr", "fr-FR"),
    GERMAN("독일어", "de", "de-DE"),
    RUSSIAN("러시아어", "ru", "ru-RU"),
    PORTUGUESE("포르투갈어", "pt", "pt-PT"),
    ITALIAN("이탈리아어", "it", "it-IT"),
    VIETNAMESE("베트남어", "vi", "vi-VN"),
    THAI("태국어", "th", "th-TH"),
    INDONESIAN("인도네시아어", "id", "id-ID"),
    HINDI("힌디어", "hi", "hi-IN"),
    ARABIC("아랍어", "ar", "ar-SA");

    private final String displayName;   // 프론트가 보내는 한글 이름
    private final String translateCode; // Google Translation API 코드 (ex: "en")
    private final String speechCode;    // Google Speech 코드 (ex: "en-US")

    SupportedLanguage(String displayName, String translateCode, String speechCode) {
        this.displayName = displayName;
        this.translateCode = translateCode;
        this.speechCode = speechCode;
    }

    // 한글 이름 또는 코드로 찾아주는 헬퍼
    public static SupportedLanguage fromClientValue(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();

        // 1) 한글 이름으로 매칭 (한국어, 영어, 일본어 …)
        for (SupportedLanguage lang : values()) {
            if (lang.displayName.equals(v)) {
                return lang;
            }
        }

        // 2) ISO 코드로 매칭 (ko, en, zh-CN …)도 허용
        for (SupportedLanguage lang : values()) {
            if (lang.translateCode.equalsIgnoreCase(v) || lang.speechCode.equalsIgnoreCase(v)) {
                return lang;
            }
        }

        return null; // 못 찾으면 null (나중에 예외로 처리)
    }
}
