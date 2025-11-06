package com.example.WayGo.Service.Translation;

import com.example.WayGo.Constant.ErrorCode;
import com.example.WayGo.Dto.Translation.ImageTranslationRequest;
import com.example.WayGo.Dto.Translation.OcrTranslationResponse;
import com.example.WayGo.Exception.TranslationException;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.translate.v3.*;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor  // final 필드들을 생성자 주입
public class OcrTranslateService {

    private final ImageAnnotatorClient visionClient;
    private final TranslationServiceClient translationClient;
    private final String gcpProjectId;

    /**
     * 이미지(Base64) → OCR → 번역
     *
     * @param req 이미지 번역 요청
     * @return OCR 추출 텍스트 + 번역 결과
     * @throws TranslationException OCR 또는 번역 실패 시
     */
    public OcrTranslationResponse translateImage(ImageTranslationRequest req) {

        try {
            // ========== 1단계: Base64 디코딩 ==========
            byte[] imgBytes;
            try {
                imgBytes = Base64.getDecoder().decode(req.getImageContent());
                log.info("Image decoded successfully: {} bytes", imgBytes.length);
            } catch (IllegalArgumentException e) {
                throw new TranslationException(ErrorCode.INVALID_REQUEST,
                        "잘못된 Base64 형식입니다", e);
            }

            Image image = Image.newBuilder()
                    .setContent(ByteString.copyFrom(imgBytes))
                    .build();

            // ========== 2단계: OCR 요청 구성 ==========
            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                    .build();

            AnnotateImageRequest ocrReq = AnnotateImageRequest.newBuilder()
                    .setImage(image)
                    .addFeatures(feature)
                    .build();

            // ========== 3단계: OCR 호출 ==========
            log.info("OCR request: mimeType={}", req.getMimeType());

            BatchAnnotateImagesResponse ocrRes = visionClient.batchAnnotateImages(
                    java.util.List.of(ocrReq)
            );

            if (ocrRes.getResponsesCount() == 0) {
                throw new TranslationException(ErrorCode.TRANSLATION_FAILED,
                        "이미지 분석 결과가 없습니다");
            }

            AnnotateImageResponse imgRes = ocrRes.getResponses(0);

            // 에러 체크
            if (imgRes.hasError()) {
                log.error("OCR error: {}", imgRes.getError().getMessage());
                throw new TranslationException(ErrorCode.FILE_READ_FAILED,
                        "이미지에서 텍스트를 추출할 수 없습니다: " + imgRes.getError().getMessage());
            }

            // ========== 4단계: OCR 텍스트 추출 ==========
            String ocrText;
            if (imgRes.hasFullTextAnnotation()) {
                ocrText = imgRes.getFullTextAnnotation().getText();
            } else if (imgRes.getTextAnnotationsCount() > 0) {
                ocrText = imgRes.getTextAnnotations(0).getDescription();
            } else {
                ocrText = "";
            }

            if (ocrText.isEmpty()) {
                throw new TranslationException(ErrorCode.TRANSLATION_FAILED,
                        "이미지에서 텍스트를 찾을 수 없습니다");
            }

            log.info("OCR successful: textLength={}", ocrText.length());

            // ========== 5단계: 번역 호출 ==========
            String location = "global";
            LocationName parent = LocationName.of(gcpProjectId, location);

            TranslateTextRequest.Builder translateReqBuilder = TranslateTextRequest.newBuilder()
                    .setParent(parent.toString())
                    .setMimeType("text/plain")
                    .setTargetLanguageCode(req.getTargetLanguage())
                    .addContents(ocrText);

            // sourceLanguage가 제공되면 명시
            if (req.getSourceLanguage() != null && !req.getSourceLanguage().isBlank()) {
                translateReqBuilder.setSourceLanguageCode(req.getSourceLanguage());
            }

            log.info("Translation request: target={}", req.getTargetLanguage());

            TranslateTextResponse translateRes = translationClient.translateText(
                    translateReqBuilder.build()
            );

            if (translateRes.getTranslationsCount() == 0) {
                throw new TranslationException(ErrorCode.TRANSLATION_FAILED, "번역 결과가 없습니다");
            }

            String translated = translateRes.getTranslations(0).getTranslatedText();
            log.info("Translation successful");

            // ========== 6단계: 응답 DTO ==========
            return OcrTranslationResponse.builder()
                    .ocrText(ocrText)
                    .translatedText(translated)
                    .build();

        } catch (ApiException e) {
            log.error("Google API error during image translation: {}", e.getMessage(), e);
            throw new TranslationException(ErrorCode.TRANSLATION_FAILED,
                    "이미지 번역 중 API 오류가 발생했습니다: " + e.getMessage(), e);
        } catch (TranslationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during image translation: {}", e.getMessage(), e);
            throw new TranslationException(ErrorCode.UNKNOWN_ERROR, e);
        }
    }
}