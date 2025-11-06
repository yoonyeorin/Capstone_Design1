package com.example.WayGo.Dto.Translation;

import lombok.*;

/**
 * 음성 번역 응답 DTO
 * - transcript: STT 결과
 * - translatedText: 번역 결과 (기존 TranslationResponse와 필드명 일치)
 * - detectedSourceLanguage: 자동 감지된 원본 언어(있다면)
 * - confidence: STT 인식 신뢰도(0~1 범위, 제공 가능 시)
 * ※ TTS(음성합성) 결과(Base64 오디오)를 붙일 계획이면 audioContent(옵션)를 추후 추가.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioTranslationResponse {

    private String transcript;
    private String translatedText;

}
