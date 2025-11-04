package com.example.WayGo.Controller.Translation;

import com.example.WayGo.Dto.Translation.*;
import com.example.WayGo.Service.Translation.TranslateService;
import com.example.WayGo.Service.Translation.SpeechTranslateService;
import com.example.WayGo.Service.Translation.OcrTranslateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/translation")          // ← 공통 prefix
@RequiredArgsConstructor                    // ← 생성자 주입 (final 필드들 자동 주입)
public class TranslationController {

    private final TranslateService translateService;          // 텍스트 번역
    private final SpeechTranslateService speechTranslateService; // 음성(STT)→번역
    private final OcrTranslateService ocrTranslateService;       // 이미지(OCR)→번역

    @PostMapping("/text")                                     // 기존 엔드포인트 유지
    public ApiResponse<TranslationResponse> translateText(
            @Valid @RequestBody TextTranslationRequest request) {
        TranslationResponse response = translateService.translateText(request);
        return ApiResponse.success(response);                 // 공통 응답 래퍼
    }

    @PostMapping("/audio")                                    // ★ 신규: 오디오 번역
    public ApiResponse<AudioTranslationResponse> translateAudio(
            @Valid @RequestBody AudioTranslationRequest request) {
        AudioTranslationResponse response = speechTranslateService.translateAudio(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/image")                                    // ★ 신규: 이미지 번역
    public ApiResponse<OcrTranslationResponse> translateImage(
            @Valid @RequestBody ImageTranslationRequest request) {
        OcrTranslationResponse response = ocrTranslateService.translateImage(request);
        return ApiResponse.success(response);
    }
}
