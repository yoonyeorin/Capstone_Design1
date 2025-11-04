package com.example.WayGo.Dto.Translation;

/**
 * STT 입력 오디오 인코딩 유형
 * Google Speech-to-Text의 주요 옵션만 우선 노출 (필요 시 추가 가능)
 */
public enum AudioEncodingType {
    LINEAR16,   // 16-bit signed little-endian
    FLAC,
    MP3,
    OGG_OPUS,
    WEBM_OPUS
}
