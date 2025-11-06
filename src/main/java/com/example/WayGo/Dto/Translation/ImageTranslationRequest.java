package com.example.WayGo.Dto.Translation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 이미지(OCR) → 텍스트 → 번역 요청 DTO
 * - imageContent: Base64 인코딩된 이미지
 * - mimeType: 이미지 포맷
 * - sourceLanguage: OCR 힌트 언어
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageTranslationRequest {

    /**
     * Base64 인코딩된 이미지 데이터
     * 최대 10MB (Base64는 원본의 약 1.33배이므로 약 13,333,333자)
     */
    @NotBlank(message = "이미지 데이터(Base64)는 필수입니다")
    @Size(max = 13_333_333, message = "이미지 파일은 10MB 이하여야 합니다")
    private String imageContent;

    /**
     * 이미지 MIME 타입
     */
    @NotNull(message = "이미지 MIME 타입은 필수입니다")
    private ImageMimeType mimeType;

    /**
     * 원본 언어 (null이면 자동 감지 시도)
     */
    private String sourceLanguage;

    /**
     * 목표 언어 (필수)
     */
    @NotBlank(message = "목표 언어는 필수입니다")
    private String targetLanguage;
}