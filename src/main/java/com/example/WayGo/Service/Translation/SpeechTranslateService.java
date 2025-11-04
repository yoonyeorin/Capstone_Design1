package com.example.WayGo.Service.Translation;

import com.example.WayGo.Constant.ErrorCode;
import com.example.WayGo.Dto.Translation.*;
import com.example.WayGo.Exception.TranslationException;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.speech.v1.*;
import com.google.cloud.translate.v3.*;
import com.google.cloud.translate.v3.LocationName;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor  // final 필드들을 생성자 주입
public class SpeechTranslateService {

    private final SpeechClient speechClient;
    private final TranslationServiceClient translationClient;
    private final String gcpProjectId;

    /**
     * 오디오(Base64) → STT → 번역
     *
     * @param req 오디오 번역 요청
     * @return 음성 인식 결과 + 번역 결과
     * @throws TranslationException STT 또는 번역 실패 시
     */
    public AudioTranslationResponse translateAudio(AudioTranslationRequest req) {

        try {
            // ========== 1단계: Base64 디코딩 ==========
            byte[] audioBytes;
            try {
                audioBytes = Base64.getDecoder().decode(req.getAudioContent());
                log.info("Audio decoded successfully: {} bytes", audioBytes.length);
            } catch (IllegalArgumentException e) {
                throw new TranslationException(ErrorCode.INVALID_REQUEST,
                        "잘못된 Base64 형식입니다", e);
            }

            ByteString audioByteString = ByteString.copyFrom(audioBytes);

            // ========== 2단계: RecognitionConfig 구성 ==========
            RecognitionConfig.Builder configBuilder = RecognitionConfig.newBuilder()
                    .setLanguageCode(req.getSourceLanguage())
                    .setEncoding(mapEncoding(req.getEncoding()))
                    .setSampleRateHertz(req.getSampleRateHertz());


            if (Boolean.TRUE.equals(req.getEnableAutomaticPunctuation())) {
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

            log.info("STT request: language={}, encoding={}",
                    req.getSourceLanguage(), req.getEncoding());

            RecognizeResponse sttResponse = speechClient.recognize(sttRequest);

            // ========== 5단계: STT 결과 합치기 ==========
            StringBuilder transcriptBuilder = new StringBuilder();
            for (SpeechRecognitionResult result : sttResponse.getResultsList()) {
                if (result.getAlternativesCount() > 0) {
                    SpeechRecognitionAlternative alt = result.getAlternatives(0);
                    transcriptBuilder.append(alt.getTranscript());
                    transcriptBuilder.append(" ");
                }
            }
            String transcript = transcriptBuilder.toString().trim();

            if (transcript.isEmpty()) {
                throw new TranslationException(ErrorCode.TRANSLATION_FAILED,
                        "음성에서 텍스트를 추출할 수 없습니다");
            }

            log.info("STT successful: transcriptLength={}", transcript.length());

            // ========== 6단계: 번역 요청 ==========
            String location = "global";
            LocationName parent = LocationName.of(gcpProjectId, location);

            TranslateTextRequest translateRequest = TranslateTextRequest.newBuilder()
                    .setParent(parent.toString())
                    .setMimeType("text/plain")
                    .setTargetLanguageCode(req.getTargetLanguage())
                    .addContents(transcript)
                    .build();

            log.info("Translation request: target={}", req.getTargetLanguage());

            TranslateTextResponse translateResponse = translationClient.translateText(translateRequest);

            if (translateResponse.getTranslationsCount() == 0) {
                throw new TranslationException(ErrorCode.TRANSLATION_FAILED, "번역 결과가 없습니다");
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
            throw new TranslationException(ErrorCode.TRANSLATION_FAILED,
                    "음성 번역 중 API 오류가 발생했습니다: " + e.getMessage(), e);
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