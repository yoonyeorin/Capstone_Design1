package com.example.WayGo.Controller;

import com.example.WayGo.Dto.*;
import com.example.WayGo.Entity.UserEntity;
import com.example.WayGo.Service.UserService;
import com.example.WayGo.Service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@Tag(name = "Profile", description = "프로필 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ProfileController {

    private final UserService userService;
    private final S3Service s3Service;

    /**
     * 프로필 사진 업로드
     */
    @Operation(summary = "프로필 사진 업로드", description = "사용자의 프로필 사진을 업로드합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파일"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping(value = "/image/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileImageResponse> uploadProfileImage(
            @Parameter(description = "프로필 이미지 파일", required = true)
            @RequestParam("file") MultipartFile file,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ProfileImageResponse(false, "로그인이 필요합니다.", null, null));
        }

        try {
            // 기존 프로필 이미지 삭제 (있는 경우)
            UserEntity user = userService.getLoginUserById(userId);
            if (user != null && user.getProfileImageUrl() != null) {
                s3Service.deleteImage(user.getProfileImageUrl());
            }

            // 새 프로필 이미지 업로드
            String imageUrl = s3Service.uploadProfileImage(file, userId);

            // 데이터베이스 업데이트
            userService.updateProfileImage(userId, imageUrl);

            log.info("프로필 사진 업로드 성공: userId={}, url={}", userId, imageUrl);

            return ResponseEntity.ok(new ProfileImageResponse(
                    true, "프로필 사진이 업로드되었습니다.", imageUrl, "profile"));

        } catch (IllegalArgumentException e) {
            log.warn("프로필 사진 업로드 실패 - 유효성 검사: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ProfileImageResponse(false, e.getMessage(), null, null));

        } catch (Exception e) {
            log.error("프로필 사진 업로드 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ProfileImageResponse(false, "이미지 업로드에 실패했습니다.", null, null));
        }
    }

    /**
     * 배경 사진 업로드
     */
    @Operation(summary = "배경 사진 업로드", description = "사용자의 배경 사진을 업로드합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파일"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping(value = "/image/background", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileImageResponse> uploadBackgroundImage(
            @Parameter(description = "배경 이미지 파일", required = true)
            @RequestParam("file") MultipartFile file,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ProfileImageResponse(false, "로그인이 필요합니다.", null, null));
        }

        try {
            // 기존 배경 이미지 삭제 (있는 경우)
            UserEntity user = userService.getLoginUserById(userId);
            if (user != null && user.getBackgroundImageUrl() != null) {
                s3Service.deleteImage(user.getBackgroundImageUrl());
            }

            // 새 배경 이미지 업로드
            String imageUrl = s3Service.uploadBackgroundImage(file, userId);

            // 데이터베이스 업데이트
            userService.updateBackgroundImage(userId, imageUrl);

            log.info("배경 사진 업로드 성공: userId={}, url={}", userId, imageUrl);

            return ResponseEntity.ok(new ProfileImageResponse(
                    true, "배경 사진이 업로드되었습니다.", imageUrl, "background"));

        } catch (IllegalArgumentException e) {
            log.warn("배경 사진 업로드 실패 - 유효성 검사: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ProfileImageResponse(false, e.getMessage(), null, null));

        } catch (Exception e) {
            log.error("배경 사진 업로드 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ProfileImageResponse(false, "이미지 업로드에 실패했습니다.", null, null));
        }
    }

    /**
     * 프로필 사진 삭제
     */
    @Operation(summary = "프로필 사진 삭제", description = "사용자의 프로필 사진을 삭제합니다.")
    @DeleteMapping("/image/profile")
    public ResponseEntity<Map<String, Object>> deleteProfileImage(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        try {
            UserEntity user = userService.getLoginUserById(userId);
            if (user != null && user.getProfileImageUrl() != null) {
                s3Service.deleteImage(user.getProfileImageUrl());
                userService.updateProfileImage(userId, null);

                log.info("프로필 사진 삭제 성공: userId={}", userId);
                return ResponseEntity.ok(Map.of("success", true, "message", "프로필 사진이 삭제되었습니다."));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "삭제할 프로필 사진이 없습니다."));
            }
        } catch (Exception e) {
            log.error("프로필 사진 삭제 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "이미지 삭제에 실패했습니다."));
        }
    }

    /**
     * 배경 사진 삭제
     */
    @Operation(summary = "배경 사진 삭제", description = "사용자의 배경 사진을 삭제합니다.")
    @DeleteMapping("/image/background")
    public ResponseEntity<Map<String, Object>> deleteBackgroundImage(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        try {
            UserEntity user = userService.getLoginUserById(userId);
            if (user != null && user.getBackgroundImageUrl() != null) {
                s3Service.deleteImage(user.getBackgroundImageUrl());
                userService.updateBackgroundImage(userId, null);

                log.info("배경 사진 삭제 성공: userId={}", userId);
                return ResponseEntity.ok(Map.of("success", true, "message", "배경 사진이 삭제되었습니다."));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "삭제할 배경 사진이 없습니다."));
            }
        } catch (Exception e) {
            log.error("배경 사진 삭제 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "이미지 삭제에 실패했습니다."));
        }
    }
}
