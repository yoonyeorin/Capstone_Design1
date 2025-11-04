package com.example.WayGo.Config.Translation;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.cloud.translate.v3.TranslationServiceSettings;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

/**
 * Google Cloud 서비스 클라이언트 설정
 * 모든 Google API 클라이언트를 Spring Bean으로 등록하여 관리
 */
@Slf4j
@Configuration
public class GoogleCloudConfig {

    private final String credentialsPath;
    private final String projectId;

    /**
     * 생성자 주입 방식
     * Value 어노테이션으로 application.properties 값을 주입받음
     */
    public GoogleCloudConfig(
            @Value("${google.cloud.credentials.location}") String credentialsPath,
            @Value("${google.cloud.project-id}") String projectId) {
        this.credentialsPath = credentialsPath;
        this.projectId = projectId;
        log.info("GoogleCloudConfig initialized with projectId: {}", projectId);
    }

    /**
     * Google 인증 정보를 로드합니다.
     * resources 경로를 classpath 기준으로 읽어옵니다.
     *
     * @return Google 인증 객체
     * @throws IOException 인증 파일을 찾을 수 없거나 읽을 수 없을 때
     */
    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        String fileName = credentialsPath.replace("classpath:", "");

        try (InputStream credentialsStream = getClass()
                .getClassLoader()
                .getResourceAsStream(fileName)) {

            if (credentialsStream == null) {
                String errorMsg = String.format(
                        "Google credentials file not found at: %s " +
                                "(확인 사항: 1) resources 폴더에 파일 존재 여부, 2) 경로 오탈자)",
                        credentialsPath
                );
                log.error(errorMsg);
                throw new IOException(errorMsg);
            }

            log.info("Google credentials loaded successfully from: {}", fileName);
            return GoogleCredentials.fromStream(credentialsStream);
        }
    }

    /**
     * Translation API 클라이언트 (텍스트 번역)
     *
     * @param credentials Google 인증 정보
     * @return Translation 서비스 클라이언트
     * @throws IOException 클라이언트 생성 실패 시
     */
    @Bean(destroyMethod = "close")
    public TranslationServiceClient translationServiceClient(GoogleCredentials credentials) throws IOException {
        TranslationServiceSettings settings = TranslationServiceSettings
                .newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build();

        log.info("TranslationServiceClient created successfully");
        return TranslationServiceClient.create(settings);
    }

    /**
     * Speech-to-Text 클라이언트 (음성 → 텍스트)
     *
     * @param credentials Google 인증 정보
     * @return Speech 클라이언트
     * @throws IOException 클라이언트 생성 실패 시
     */
    @Bean(destroyMethod = "close")
    public SpeechClient speechClient(GoogleCredentials credentials) throws IOException {
        SpeechSettings settings = SpeechSettings.newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build();

        log.info("SpeechClient created successfully");
        return SpeechClient.create(settings);
    }

    /**
     * Text-to-Speech 클라이언트 (텍스트 → 음성)
     * 번역된 결과를 음성으로 반환하고 싶을 때 사용
     *
     * @param credentials Google 인증 정보
     * @return Text-to-Speech 클라이언트
     * @throws IOException 클라이언트 생성 실패 시
     */
    @Bean(destroyMethod = "close")
    public TextToSpeechClient textToSpeechClient(GoogleCredentials credentials) throws IOException {
        TextToSpeechSettings settings = TextToSpeechSettings.newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build();

        log.info("TextToSpeechClient created successfully");
        return TextToSpeechClient.create(settings);
    }

    /**
     * Vision API 클라이언트 (이미지 OCR)
     * 이미지에서 텍스트 추출 후 번역 파이프라인에 사용
     *
     * @param credentials Google 인증 정보
     * @return Image Annotator 클라이언트
     * @throws IOException 클라이언트 생성 실패 시
     */
    @Bean(destroyMethod = "close")
    public ImageAnnotatorClient imageAnnotatorClient(GoogleCredentials credentials) throws IOException {
        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build();

        log.info("ImageAnnotatorClient created successfully");
        return ImageAnnotatorClient.create(settings);
    }

    /**
     * GCP 프로젝트 ID를 Bean으로 제공
     * 여러 서비스에서 필요할 수 있어 별도 Bean으로 분리
     *
     * @return GCP 프로젝트 ID
     */
    @Bean
    public String gcpProjectId() {
        log.info("GCP Project ID Bean created: {}", projectId);
        return projectId;
    }
}