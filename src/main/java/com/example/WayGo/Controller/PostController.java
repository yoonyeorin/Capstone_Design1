package com.example.WayGo.Controller;

import com.example.WayGo.Dto.PostRequest;
import com.example.WayGo.Dto.PostResponse;
import com.example.WayGo.Dto.PostListResponse;
import com.example.WayGo.Service.PostService;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "Post", description = "게시글 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PostController {

    private final PostService postService;

    /**
     * 게시글 작성 (이미지 포함)
     */
    @Operation(summary = "게시글 작성", description = "새로운 게시글을 작성합니다. 최대 5개의 이미지 업로드 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "게시글 작성 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 오류"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> createPost(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다."
            ));
        }

        // 입력값 검증
        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "제목이 비어있습니다."
            ));
        }

        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "내용이 비어있습니다."
            ));
        }

        if (title.length() > 200) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "제목은 200자 이하로 입력해주세요."
            ));
        }

        if (content.length() > 10000) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "내용은 10000자 이하로 입력해주세요."
            ));
        }

        // 이미지 개수 제한 (최대 5개)
        if (images != null && images.size() > 5) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "이미지는 최대 5개까지 업로드 가능합니다."
            ));
        }

        try {
            PostRequest request = new PostRequest();
            request.setTitle(title);
            request.setContent(content);

            PostResponse post = postService.createPost(userId, request, images);
            log.info("게시글 작성 성공: postId={}, userId={}", post.getId(), userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "게시글이 성공적으로 작성되었습니다.");
            response.put("post", post);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("게시글 작성 중 오류: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "게시글 작성 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 게시글 수정 (이미지 추가 가능)
     */
    @Operation(summary = "게시글 수정", description = "게시글을 수정합니다. 새로운 이미지를 추가할 수 있습니다.")
    @PutMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> updatePost(
            @PathVariable Long postId,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다."
            ));
        }

        // 입력값 검증
        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "제목이 비어있습니다."
            ));
        }

        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "내용이 비어있습니다."
            ));
        }

        try {
            PostRequest request = new PostRequest();
            request.setTitle(title);
            request.setContent(content);

            PostResponse post = postService.updatePost(postId, userId, request, images);
            log.info("게시글 수정 성공: postId={}, userId={}", postId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "게시글이 성공적으로 수정되었습니다.");
            response.put("post", post);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("게시글 수정 실패: postId={}, userId={}, error={}", postId, userId, e.getMessage());
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
            log.error("게시글 수정 중 오류: postId={}, userId={}, error={}", postId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "게시글 수정 중 오류가 발생했습니다."
            ));
        }
    }

    // 나머지 메서드들은 기존 코드와 동일...

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        // 기존 코드 유지
        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("desc")
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

            Page<PostListResponse> posts = postService.getAllPosts(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "content", posts.getContent(),
                    "totalElements", posts.getTotalElements(),
                    "totalPages", posts.getTotalPages(),
                    "currentPage", posts.getNumber(),
                    "size", posts.getSize(),
                    "first", posts.isFirst(),
                    "last", posts.isLast()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("게시글 목록 조회 중 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "게시글 목록 조회 중 오류가 발생했습니다."
            ));
        }
    }

    @GetMapping("/{postId}")
    public ResponseEntity<Map<String, Object>> getPost(@PathVariable Long postId) {
        try {
            PostResponse post = postService.getPost(postId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("post", post);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("게시글 조회 실패: postId={}, error={}", postId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("게시글 조회 중 오류: postId={}, error={}", postId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "게시글 조회 중 오류가 발생했습니다."
            ));
        }
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Map<String, Object>> deletePost(
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
            postService.deletePost(postId, userId);
            log.info("게시글 삭제 성공: postId={}, userId={}", postId, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "게시글이 성공적으로 삭제되었습니다."
            ));

        } catch (RuntimeException e) {
            log.warn("게시글 삭제 실패: postId={}, userId={}, error={}", postId, userId, e.getMessage());
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
            log.error("게시글 삭제 중 오류: postId={}, userId={}, error={}", postId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "게시글 삭제 중 오류가 발생했습니다."
            ));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyPosts(
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
            Page<PostListResponse> posts = postService.getMyPosts(userId, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "content", posts.getContent(),
                    "totalElements", posts.getTotalElements(),
                    "totalPages", posts.getTotalPages(),
                    "currentPage", posts.getNumber()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("내 게시글 조회 중 오류: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "게시글 조회 중 오류가 발생했습니다."
            ));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PostListResponse> posts = postService.searchPosts(keyword, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("keyword", keyword);
            response.put("data", Map.of(
                    "content", posts.getContent(),
                    "totalElements", posts.getTotalElements(),
                    "totalPages", posts.getTotalPages(),
                    "currentPage", posts.getNumber()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("게시글 검색 중 오류: keyword={}, error={}", keyword, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "게시글 검색 중 오류가 발생했습니다."
            ));
        }
    }
}