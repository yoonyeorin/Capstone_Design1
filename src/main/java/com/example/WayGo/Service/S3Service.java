package com.example.WayGo.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.UUID;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${spring.cloud.aws.s3.region}")
    private String region;

    // 허용되는 이미지 확장자
    private final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    // 최대 파일 크기 (5MB)
    private final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * 프로필 이미지 업로드
     */
    public String uploadProfileImage(MultipartFile file, Long userId) throws IOException {
        return uploadImage(file, "profile", userId);
    }

    /**
     * 배경 이미지 업로드
     */
    public String uploadBackgroundImage(MultipartFile file, Long userId) throws IOException {
        return uploadImage(file, "background", userId);
    }

    /**
     * 이미지 업로드 공통 메서드
     */
    private String uploadImage(MultipartFile file, String imageType, Long userId) throws IOException {
        // 파일 유효성 검사
        validateFile(file);

        // 파일명 생성 (중복 방지를 위한 UUID 사용)
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String fileName = String.format("%s/%d/%s_%s.%s",
                imageType, userId, imageType, UUID.randomUUID().toString(), extension);

        try {
            // S3에 파일 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // 업로드된 파일의 URL 반환
            String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                    bucketName, region, fileName);

            log.info("{} 이미지 업로드 성공: userId={}, url={}", imageType, userId, imageUrl);
            return imageUrl;

        } catch (Exception e) {
            log.error("{} 이미지 업로드 실패: userId={}, error={}", imageType, userId, e.getMessage());
            throw new RuntimeException("이미지 업로드에 실패했습니다.", e);
        }
    }

    /**
     * S3에서 이미지 삭제
     */
    public void deleteImage(String imageUrl) {
        try {
            // URL에서 키 추출
            String key = extractKeyFromUrl(imageUrl);
            if (key != null) {
                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();

                s3Client.deleteObject(deleteObjectRequest);
                log.info("이미지 삭제 성공: {}", imageUrl);
            }
        } catch (Exception e) {
            log.error("이미지 삭제 실패: url={}, error={}", imageUrl, e.getMessage());
        }
    }

    /**
     * 파일 유효성 검사
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기는 5MB를 초과할 수 없습니다.");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif, webp만 허용)");
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("유효하지 않은 파일명입니다.");
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * URL에서 S3 키 추출
     */
    private String extractKeyFromUrl(String url) {
        try {
            String baseUrl = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
            if (url.startsWith(baseUrl)) {
                return url.substring(baseUrl.length());
            }
        } catch (Exception e) {
            log.error("URL에서 키 추출 실패: {}", url);
        }
        return null;
    }
}