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

    @Value("${google.cloud.project-id}")
    private String projectId;

    /**
     * 텍스트를 번역합니다
     *
     * @param request 번역 요청 (텍스트, 원본 언어, 목표 언어)
     * @return 번역 결과
     */
    public TranslationResponse translateText(TextTranslationRequest request) {

        // ========== 1단계: Request에서 데이터 꺼내기 ==========
        String text = request.getText();                        // "안녕하세요"
        String sourceLanguage = request.getSourceLanguage();    // "ko" 또는 null
        String targetLanguage = request.getTargetLanguage();    // "en"


        // ========== 2단계: Google API 위치 설정 ==========
        String location = "global";
        LocationName parent = LocationName.of(projectId, location);
        // 결과: "projects/onyx-pad-458806/locations/global"


        // ========== 3단계: 번역 요청 객체 만들기 ==========
        TranslateTextRequest.Builder requestBuilder = TranslateTextRequest.newBuilder()
                .setParent(parent.toString())              // 위치 설정
                .setMimeType("text/plain")                 // 텍스트 타입
                .setTargetLanguageCode(targetLanguage)     // 목표 언어: "en"
                .addContents(text);                        // 번역할 텍스트

        // 원본 언어가 있으면 추가 (없으면 자동 감지)
        if (sourceLanguage != null && !sourceLanguage.isEmpty()) {
            requestBuilder.setSourceLanguageCode(sourceLanguage);
        }

        TranslateTextRequest translateRequest = requestBuilder.build();


        // ========== 4단계: Google API 호출 (진짜 번역!) ==========
        TranslateTextResponse response = translationClient.translateText(translateRequest);
        // 네트워크 통신 발생!
        // Google 서버에 요청 보내고 응답 받기


        // ========== 5단계: 결과 추출 ==========
        String translatedText = response.getTranslations(0).getTranslatedText();
        // Google이 보내준 번역 결과: "Hello"


        // ========== 6단계: Response DTO 만들어서 반환 ==========
        return TranslationResponse.builder()
                .translatedText(translatedText)
                .build();
    }
}