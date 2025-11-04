package com.example.WayGo.Dto.Translation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 텍스트 번역 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TextTranslationRequest {

    /**
     * 번역할 텍스트 (필수)
     * 최대 5000자
     */
    @NotBlank(message = "번역할 텍스트는 필수입니다")
    @Size(max = 5000, message = "텍스트는 5000자 이하여야 합니다")
    private String text;

    /**
     * 원본 언어 코드 (선택, null이면 자동 감지)
     * 예: "ko", "en", "ja"
     */
    private String sourceLanguage;

    /**
     * 목표 언어 코드 (필수)
     * 예: "en", "ja", "zh"
     */
    @NotBlank(message = "목표 언어는 필수입니다")
    private String targetLanguage;
}