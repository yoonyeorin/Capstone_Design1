package com.example.WayGo.Controller;

import com.example.WayGo.Dto.CommentRequest;
import com.example.WayGo.Dto.CommentResponse;
import com.example.WayGo.Service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "Comment", description = "댓글 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/comments")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CommentController {

    private final CommentService commentService;

    /**
     * 댓글 작성
     */
    @Operation(summary = "댓글 작성", description = "게시글에 댓글 또는 답글을 작성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "댓글 작성 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 오류"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping
    public ResponseEntity<Map<String, Object>> createComment(
            @Parameter(description = "게시글 ID", required = true)
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request,
            HttpSession session,
            BindingResult bindingResult) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다."
            ));
        }

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .findFirst()
                    .orElse("입력값을 확인해주세요.");

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", errorMessage
            ));
        }

        try {
            CommentResponse comment = commentService.createComment(userId, postId, request);
            log.info("댓글 작성 성공: commentId={}, postId={}, userId={}",
                    comment.getId(), postId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", request.getParentId() != null ?
                    "답글이 작성되었습니다." : "댓글이 작성되었습니다.");
            response.put("comment", comment);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            log.warn("댓글 작성 실패: postId={}, userId={}, error={}", postId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("댓글 작성 중 오류: postId={}, userId={}, error={}", postId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "댓글 작성 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 게시글의 댓글 목록 조회
     */
    @Operation(summary = "댓글 목록 조회", description = "게시글의 모든 댓글과 답글을 조회합니다.")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getComments(
            @Parameter(description = "게시글 ID", required = true)
            @PathVariable Long postId,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");

        try {
            List<CommentResponse> comments = commentService.getCommentsByPostId(postId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("comments", comments);
            response.put("totalCount", comments.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("댓글 조회 중 오류: postId={}, error={}", postId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "댓글 조회 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 댓글 수정
     */
    @Operation(summary = "댓글 수정", description = "댓글 내용을 수정합니다. 작성자만 수정 가능합니다.")
    @PutMapping("/{commentId}")
    public ResponseEntity<Map<String, Object>> updateComment(
            @PathVariable Long postId,
            @Parameter(description = "댓글 ID", required = true)
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request,
            HttpSession session,
            BindingResult bindingResult) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다."
            ));
        }

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .findFirst()
                    .orElse("입력값을 확인해주세요.");

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", errorMessage
            ));
        }

        try {
            CommentResponse comment = commentService.updateComment(commentId, userId, request);
            log.info("댓글 수정 성공: commentId={}, userId={}", commentId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "댓글이 수정되었습니다.");
            response.put("comment", comment);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("댓글 수정 실패: commentId={}, userId={}, error={}", commentId, userId, e.getMessage());
            if (e.getMessage().contains("권한이 없습니다")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "success", false,
                        "message", e.getMessage()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "success", false,
                        "message", e.getMessage()
                ));
            }
        } catch (Exception e) {
            log.error("댓글 수정 중 오류: commentId={}, userId={}, error={}", commentId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "댓글 수정 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 댓글 삭제
     */
    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다. 답글이 있으면 소프트 삭제, 없으면 완전 삭제됩니다.")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable Long postId,
            @Parameter(description = "댓글 ID", required = true)
            @PathVariable Long commentId,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다."
            ));
        }

        try {
            commentService.deleteComment(commentId, userId);
            log.info("댓글 삭제 성공: commentId={}, userId={}", commentId, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "댓글이 삭제되었습니다."
            ));

        } catch (RuntimeException e) {
            log.warn("댓글 삭제 실패: commentId={}, userId={}, error={}", commentId, userId, e.getMessage());
            if (e.getMessage().contains("권한이 없습니다")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "success", false,
                        "message", e.getMessage()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "success", false,
                        "message", e.getMessage()
                ));
            }
        } catch (Exception e) {
            log.error("댓글 삭제 중 오류: commentId={}, userId={}, error={}", commentId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "댓글 삭제 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 내가 작성한 댓글 조회
     */
    @Operation(summary = "내 댓글 조회", description = "현재 로그인한 사용자가 작성한 댓글 목록을 조회합니다.")
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다."
            ));
        }

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<CommentResponse> comments = commentService.getMyComments(userId, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "content", comments.getContent(),
                    "totalElements", comments.getTotalElements(),
                    "totalPages", comments.getTotalPages(),
                    "currentPage", comments.getNumber()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("내 댓글 조회 중 오류: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "댓글 조회 중 오류가 발생했습니다."
            ));
        }
    }
}
