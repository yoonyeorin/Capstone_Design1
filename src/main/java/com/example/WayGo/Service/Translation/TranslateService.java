package com.example.WayGo.Service.Translation;

import com.example.WayGo.Dto.Translation.TextTranslationRequest;
import com.example.WayGo.Dto.Translation.TranslationResponse;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.TranslationServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Google Translation API를 사용한 번역 서비스
 */
@Service
@RequiredArgsConstructor
public class TranslateService {

    private final TranslationServiceClient translationClient;
    private final LanguageMappingService languageMappingService;

    @Value("${google.cloud.project-id}")
    private String projectId;

    /**
     * 텍스트를 번역합니다
     *
     * @param request 번역 요청 (텍스트, 원본 언어, 목표 언어)
     * @return 번역 결과
     */
    public TranslationResponse translateText(TextTranslationRequest request) {

        // 1) Request 데이터
        String text = request.getText();                        // "안녕하세요"
        String sourceLanguageClient = request.getSourceLanguage();    // "한국어" or "ko" or null
        String targetLanguageClient = request.getTargetLanguage();    // "영어" or "en"

        // 2) 언어 코드 매핑
        // targetLanguage는 필수
        String targetLanguage = languageMappingService.toTranslateCode(targetLanguageClient);

        // sourceLanguage는 선택(없으면 자동 감지)
        String sourceLanguage = null;
        if (sourceLanguageClient != null && !sourceLanguageClient.isBlank()) {
            sourceLanguage = languageMappingService.toTranslateCode(sourceLanguageClient);
        }

        // 3) Google API 위치 설정
        String location = "global";
        LocationName parent = LocationName.of(projectId, location);

        // 4) 번역 요청 객체
        TranslateTextRequest.Builder requestBuilder = TranslateTextRequest.newBuilder()
                .setParent(parent.toString())
                .setMimeType("text/plain")
                .setTargetLanguageCode(targetLanguage)
                .addContents(text);

        if (sourceLanguage != null) {
            requestBuilder.setSourceLanguageCode(sourceLanguage);
        }

        TranslateTextRequest translateRequest = requestBuilder.build();

        // 5) Google API 호출
        TranslateTextResponse response = translationClient.translateText(translateRequest);

        // 6) 결과 추출
        String translatedText = response.getTranslations(0).getTranslatedText();

        return TranslationResponse.builder()
                .translatedText(translatedText)
                .build();
    }
}
