package com.example.WayGo.Dto.Translation;

import lombok.*;

/**
 * 이미지 번역 응답 DTO
 * - ocrText: OCR로 추출된 원문 텍스트
 * - translatedText: 번역 결과 (일관성 유지)
 * - detectedSourceLanguage: 자동 감지된 원본 언어(있다면)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrTranslationResponse {

    private String ocrText;
    private String translatedText;

}
