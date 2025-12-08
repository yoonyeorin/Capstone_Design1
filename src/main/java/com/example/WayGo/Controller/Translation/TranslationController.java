package com.example.WayGo.Controller.Translation;

import com.example.WayGo.Dto.Translation.ApiResponse;
import com.example.WayGo.Dto.Translation.AudioEncodingType;
import com.example.WayGo.Dto.Translation.AudioTranslationResponse;
import com.example.WayGo.Dto.Translation.OcrTranslationResponse;
import com.example.WayGo.Dto.Translation.TextTranslationRequest;
import com.example.WayGo.Dto.Translation.TranslationResponse;
import com.example.WayGo.Service.Translation.OcrTranslateService;
import com.example.WayGo.Service.Translation.SpeechTranslateService;
import com.example.WayGo.Service.Translation.TranslateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/translation")
@RequiredArgsConstructor
@Tag(
        name = "Translation API",
        description = "텍스트, 음성(STT), 이미지(OCR) -> Google Cloud API"
)
public class TranslationController {

    private final TranslateService translateService;              // 텍스트 번역
    private final SpeechTranslateService speechTranslateService;  // 음성(STT) → 번역
    private final OcrTranslateService ocrTranslateService;        // 이미지(OCR) → 번역

    // ───────────────────────── 텍스트 번역 ─────────────────────────

    @Operation(
            summary = "텍스트 번역",
            description = "Google Translation API로 순수 텍스트를 번역합니다. sourceLanguage가 비어 있으면 자동 감지합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "번역 성공",
                    content = @Content(schema = @Schema(implementation = TranslationResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("/text")
    public ApiResponse<TranslationResponse> translateText(
            @Valid @RequestBody TextTranslationRequest request
    ) {
        TranslationResponse response = translateService.translateText(request);
        return ApiResponse.success(response);
    }

    // ───────────────────────── 음성(STT) → 번역 ─────────────────────────

    @Operation(
            summary = "음성(STT) → 텍스트 → 번역",
            description = """
                    업로드한 음성 파일을 Google Speech-to-Text로 인식한 뒤,<br/>
                    인식된 텍스트를 Google Translation API로 번역합니다.<br/><br/>
                    예시:<br/>
                    - encoding=LINEAR16, sampleRateHertz=16000 인 WAV(PCM)<br/>
                    - encoding=MP3, sampleRateHertz=16000 인 MP3 파일 등
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "STT + 번역 성공",
                    content = @Content(schema = @Schema(implementation = AudioTranslationResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 파라미터 오류 (파일 누락, 인코딩/샘플레이트 문제 등)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Google Speech/Translation API 오류 또는 서버 오류",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping(
            value = "/audio",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<AudioTranslationResponse> translateAudio(
            @Parameter(
                    description = "녹음된 음성 파일 (wav, mp3 등)",
                    required = true
            )
            @RequestPart("file") MultipartFile file,

            @Parameter(
                    description = "원본 언어 (예: 한국어, 영어, ko-KR, en-US)",
                    example = "한국어",
                    required = true
            )
            @RequestParam("sourceLanguage") String sourceLanguage,

            @Parameter(
                    description = "목표 언어 (예: 영어, 일본어, en, ja, zh-CN)",
                    example = "영어",
                    required = true
            )
            @RequestParam("targetLanguage") String targetLanguage,

            @Parameter(
                    description = "오디오 인코딩 유형 (AudioEncodingType Enum)",
                    example = "MP3",
                    required = true,
                    schema = @Schema(implementation = AudioEncodingType.class)
            )
            @RequestParam("encoding") AudioEncodingType encoding,

            @Parameter(
                    description = "샘플링 레이트(Hz). 예: 16000",
                    example = "16000",
                    required = true
            )
            @RequestParam("sampleRateHertz") Integer sampleRateHertz,

            @Parameter(
                    description = "자동 구두점 사용 여부 (true/false). null이면 기본값 false",
                    example = "true",
                    required = false
            )
            @RequestParam(value = "enableAutomaticPunctuation", required = false)
            Boolean enableAutomaticPunctuation
    ) throws Exception {

        byte[] audioBytes = file.getBytes();

        AudioTranslationResponse response = speechTranslateService.translateAudio(
                audioBytes,
                sourceLanguage,      // "한국어" 같은 값 → 서비스에서 ko-KR 로 매핑
                targetLanguage,      // "영어" → en
                encoding,
                sampleRateHertz,
                enableAutomaticPunctuation
        );

        return ApiResponse.success(response);
    }

    // ───────────────────────── 이미지(OCR) → 번역 ─────────────────────────

    @Operation(
            summary = "이미지(OCR) → 텍스트 → 번역",
            description = """
                    업로드한 이미지에서 Google Vision API로 텍스트를 추출(OCR)한 뒤,<br/>
                    추출된 텍스트를 Google Translation API로 번역합니다.<br/><br/>
                    메뉴판, 안내문, 표지판 등 텍스트가 분명한 이미지에 적합합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OCR + 번역 성공",
                    content = @Content(schema = @Schema(implementation = OcrTranslationResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 오류 (파일 누락, 텍스트 미검출 등)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Google Vision/Translation API 오류 또는 서버 오류",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping(
            value = "/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<OcrTranslationResponse> translateImage(
            @Parameter(
                    description = "텍스트가 포함된 이미지 파일 (png, jpg 등)",
                    required = true
            )
            @RequestPart("file") MultipartFile file,

            @Parameter(
                    description = "목표 언어 (예: 한국어, 영어, 일본어, ko, en, ja)",
                    example = "한국어",
                    required = true
            )
            @RequestParam("targetLanguage") String targetLanguage,

            @Parameter(
                    description = "원본 언어 (선택, 비워두면 자동 감지. 예: 영어, 일본어, en, ja)",
                    example = "영어",
                    required = false
            )
            @RequestParam(value = "sourceLanguage", required = false) String sourceLanguage
    ) throws Exception {

        byte[] imgBytes = file.getBytes();
        String mimeType = file.getContentType();   // 예: "image/png"

        OcrTranslationResponse response = ocrTranslateService.translateImage(
                imgBytes,
                mimeType,
                sourceLanguage,
                targetLanguage
        );

        return ApiResponse.success(response);
    }
}
