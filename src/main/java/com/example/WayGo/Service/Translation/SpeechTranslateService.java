package com.example.WayGo.Service.Translation;

import com.example.WayGo.Constant.ErrorCode;
import com.example.WayGo.Dto.Translation.AudioEncodingType;
import com.example.WayGo.Dto.Translation.AudioTranslationResponse;
import com.example.WayGo.Exception.TranslationException;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.speech.v1.*;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechTranslateService {

    private final SpeechClient speechClient;
    private final TranslationServiceClient translationClient;
    private final String gcpProjectId;
    private final LanguageMappingService languageMappingService;

    /**
     * 오디오(byte[]) → STT → 번역
     *
     * @param audioBytes 녹음 파일 바이트
     * @param sourceLanguageClient 프론트에서 온 언어값(예: "한국어", "영어", "ko-KR" 등)
     * @param targetLanguageClient 프론트에서 온 언어값(예: "영어", "en")
     */
    public AudioTranslationResponse translateAudio(
            byte[] audioBytes,
            String sourceLanguageClient,
            String targetLanguageClient,
            AudioEncodingType encoding,
            Integer sampleRateHertz,
            Boolean enableAutomaticPunctuation
    ) {

        try {
            // ========== 1단계: 입력 검증 ==========
            if (audioBytes == null || audioBytes.length == 0) {
                throw new TranslationException(
                        ErrorCode.INVALID_REQUEST,
                        "오디오 데이터가 비어 있습니다"
                );
            }

            if (sampleRateHertz == null || sampleRateHertz < 8000) {
                throw new TranslationException(
                        ErrorCode.INVALID_REQUEST,
                        "유효하지 않은 샘플링 레이트입니다"
                );
            }

            // 프론트 값 → 실제 코드로 변환
            String sttLanguageCode = languageMappingService.toSpeechCode(sourceLanguageClient);
            String targetLanguage = languageMappingService.toTranslateCode(targetLanguageClient);

            ByteString audioByteString = ByteString.copyFrom(audioBytes);
            log.info("Audio length: {} bytes", audioBytes.length);

            // ========== 2단계: RecognitionConfig 구성 ==========
            RecognitionConfig.Builder configBuilder = RecognitionConfig.newBuilder()
                    .setLanguageCode(sttLanguageCode)
                    .setEncoding(mapEncoding(encoding))
                    .setSampleRateHertz(sampleRateHertz);

            if (Boolean.TRUE.equals(enableAutomaticPunctuation)) {
                configBuilder.setEnableAutomaticPunctuation(true);
            }

            RecognitionConfig config = configBuilder.build();

            // ========== 3단계: RecognitionAudio 구성 ==========
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(audioByteString)
                    .build();

            // ========== 4단계: STT 요청/응답 ==========
            RecognizeRequest sttRequest = RecognizeRequest.newBuilder()
                    .setConfig(config)
                    .setAudio(audio)
                    .build();

            log.info("STT request: language={}, encoding={}", sttLanguageCode, encoding);

            RecognizeResponse sttResponse = speechClient.recognize(sttRequest);

            // ========== 5단계: STT 결과 합치기 ==========
            StringBuilder transcriptBuilder = new StringBuilder();
            for (SpeechRecognitionResult result : sttResponse.getResultsList()) {
                if (result.getAlternativesCount() > 0) {
                    SpeechRecognitionAlternative alt = result.getAlternatives(0);
                    transcriptBuilder.append(alt.getTranscript()).append(" ");
                }
            }
            String transcript = transcriptBuilder.toString().trim();

            if (transcript.isEmpty()) {
                throw new TranslationException(
                        ErrorCode.TRANSLATION_FAILED,
                        "음성에서 텍스트를 추출할 수 없습니다"
                );
            }

            log.info("STT successful: transcriptLength={}", transcript.length());

            // ========== 6단계: 번역 요청 ==========
            String location = "global";
            LocationName parent = LocationName.of(gcpProjectId, location);

            TranslateTextRequest translateRequest = TranslateTextRequest.newBuilder()
                    .setParent(parent.toString())
                    .setMimeType("text/plain")
                    .setTargetLanguageCode(targetLanguage)
                    .addContents(transcript)
                    .build();

            log.info("Translation request: target={}", targetLanguage);

            TranslateTextResponse translateResponse = translationClient.translateText(translateRequest);

            if (translateResponse.getTranslationsCount() == 0) {
                throw new TranslationException(
                        ErrorCode.TRANSLATION_FAILED,
                        "번역 결과가 없습니다"
                );
            }

            String translated = translateResponse.getTranslations(0).getTranslatedText();
            log.info("Translation successful");

            // ========== 7단계: 응답 DTO 구성 ==========
            return AudioTranslationResponse.builder()
                    .transcript(transcript)
                    .translatedText(translated)
                    .build();

        } catch (ApiException e) {
            log.error("Google API error during audio translation: {}", e.getMessage(), e);
            throw new TranslationException(
                    ErrorCode.TRANSLATION_FAILED,
                    "음성 번역 중 API 오류가 발생했습니다: " + e.getMessage(),
                    e
            );
        } catch (TranslationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during audio translation: {}", e.getMessage(), e);
            throw new TranslationException(ErrorCode.UNKNOWN_ERROR, e);
        }
    }

    /**
     * enum → Google Speech API 인코딩 매핑
     */
    private RecognitionConfig.AudioEncoding mapEncoding(AudioEncodingType type) {
        return switch (type) {
            case LINEAR16 -> RecognitionConfig.AudioEncoding.LINEAR16;
            case FLAC -> RecognitionConfig.AudioEncoding.FLAC;
            case MP3 -> RecognitionConfig.AudioEncoding.MP3;
            case OGG_OPUS -> RecognitionConfig.AudioEncoding.OGG_OPUS;
            case WEBM_OPUS -> RecognitionConfig.AudioEncoding.WEBM_OPUS;
        };
    }
}
