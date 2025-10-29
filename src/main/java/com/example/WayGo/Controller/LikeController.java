package com.example.WayGo.Controller;

import com.example.WayGo.Dto.LikedPostResponse;
import com.example.WayGo.Service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "Like", description = "좋아요 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class LikeController {

    private final LikeService likeService;

    /**
     * 좋아요 추가
     */
    @Operation(summary = "좋아요 추가", description = "게시글에 좋아요를 추가합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "좋아요 추가 성공"),
            @ApiResponse(responseCode = "400", description = "이미 좋아요가 존재하거나 잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @PostMapping("/{postId}/like")
    public ResponseEntity<Map<String, Object>> addLike(
            @Parameter(description = "게시글 ID", required = true)
            @PathVariable Long postId,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다."
            ));
        }

        try {
            boolean success = likeService.addLike(userId, postId);

            if (success) {
                long likeCount = likeService.getLikeCount(postId);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "좋아요가 추가되었습니다.");
                response.put("liked", true);
                response.put("likeCount", likeCount);

                log.info("좋아요 추가 성공: userId={}, postId={}", userId, postId);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "이미 좋아요가 존재합니다."
                ));
            }

        } catch (RuntimeException e) {
            log.warn("좋아요 추가 실패: userId={}, postId={}, error={}", userId, postId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("좋아요 추가 중 오류: userId={}, postId={}, error={}", userId, postId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "좋아요 추가 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 좋아요 취소
     */
    @Operation(summary = "좋아요 취소", description = "게시글의 좋아요를 취소합니다.")
    @DeleteMapping("/{postId}/like")
    public ResponseEntity<Map<String, Object>> removeLike(
            @Parameter(description = "게시글 ID", required = true)
            @PathVariable Long postId,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다."
            ));
        }

        try {
            boolean success = likeService.removeLike(userId, postId);

            if (success) {
                long likeCount = likeService.getLikeCount(postId);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "좋아요가 취소되었습니다.");
                response.put("liked", false);
                response.put("likeCount", likeCount);

                log.info("좋아요 취소 성공: userId={}, postId={}", userId, postId);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "좋아요가 존재하지 않습니다."
                ));
            }

        } catch (RuntimeException e) {
            log.warn("좋아요 취소 실패: userId={}, postId={}, error={}", userId, postId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("좋아요 취소 중 오류: userId={}, postId={}, error={}", userId, postId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "좋아요 취소 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 좋아요 상태 확인
     */
    @Operation(summary = "좋아요 상태 확인", description = "사용자가 해당 게시글에 좋아요를 눌렀는지 확인합니다.")
    @GetMapping("/{postId}/like/status")
    public ResponseEntity<Map<String, Object>> getLikeStatus(
            @Parameter(description = "게시글 ID", required = true)
            @PathVariable Long postId,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");

        try {
            boolean isLiked = false;
            if (userId != null) {
                isLiked = likeService.isLiked(userId, postId);
            }

            long likeCount = likeService.getLikeCount(postId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("liked", isLiked);
            response.put("likeCount", likeCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("좋아요 상태 확인 중 오류: postId={}, error={}", postId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "좋아요 상태 확인 중 오류가 발생했습니다."
            ));
        }
    }
}
