package com.example.WayGo.Dto.Translation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * 음성(STT) → 텍스트 → 번역 요청 DTO
 * - audioContent: Base64 인코딩된 원본 오디오 바이트
 * - sourceLanguage: 원본 언어 코드 (null이면 자동 감지 시도)
 * - targetLanguage: 목표 언어 코드(필수)
 * - encsoding, sampleRateHertz: STT 설정
 *
 * ※ JSON 기반 통일을 위해 Base64를 선택. 멀티파트 전송은 컨트롤러에서 별도 DTO로 확장 가능.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioTranslationRequest {

    /**
     * Base64 인코딩된 오디오 데이터
     */
    @NotBlank(message = "오디오 데이터(Base64)는 필수입니다")
    private String audioContent;

    /**
     * 원본 언어 (예: "ko", "en") - null이면 자동 감지 시도
     */
    private String sourceLanguage;

    /**
     * 목표 언어 (예: "en", "ja", "zh") - 필수
     */
    @NotBlank(message = "목표 언어는 필수입니다")
    private String targetLanguage;

    /**
     * 오디오 인코딩 유형 (예: LINEAR16, MP3)
     */
    @NotNull(message = "오디오 인코딩은 필수입니다")
    private AudioEncodingType encoding;

    /**
     * 자동 구두점 삽입
     */
    private Boolean enableAutomaticPunctuation;

    @NotNull(message = "샘플링 레이트는 필수입니다")  // ← 이 필드 추가했나요?
    @Min(value = 8000, message = "샘플링 레이트는 최소 8000Hz 이상이어야 합니다")
    private Integer sampleRateHertz;  // ← 이 줄

}
