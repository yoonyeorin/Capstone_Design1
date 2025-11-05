package com.example.WayGo.Dto.Translation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 번역 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationResponse {

    /**
     * 번역된 텍스트
     */
    private String translatedText;
}