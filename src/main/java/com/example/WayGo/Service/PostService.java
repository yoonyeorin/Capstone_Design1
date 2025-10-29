package com.example.WayGo.Service;

import com.example.WayGo.Dto.PostRequest;
import com.example.WayGo.Dto.PostResponse;
import com.example.WayGo.Dto.PostListResponse;
import com.example.WayGo.Entity.PostEntity;
import com.example.WayGo.Entity.UserEntity;
import com.example.WayGo.Repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserService userService;
    private final S3Service s3Service;

    /**
     * 게시글 작성 (이미지 포함)
     */
    public PostResponse createPost(Long userId, PostRequest request, List<MultipartFile> images) throws IOException {
        UserEntity author = userService.getLoginUserById(userId);
        if (author == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        // 이미지 업로드
        List<String> imageUrls = null;
        if (images != null && !images.isEmpty()) {
            imageUrls = s3Service.uploadPostImages(images, userId);
        }

        PostEntity post = PostEntity.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .author(author)
                .viewCount(0L)
                .likeCount(0L)
                .commentCount(0L)
                .imageUrls(imageUrls)
                .build();

        PostEntity savedPost = postRepository.save(post);
        log.info("게시글 작성 완료: postId={}, userId={}, imageCount={}",
                savedPost.getId(), userId, imageUrls != null ? imageUrls.size() : 0);

        return convertToPostResponse(savedPost);
    }

    /**
     * 게시글 수정 (이미지 포함)
     */
    public PostResponse updatePost(Long postId, Long userId, PostRequest request, List<MultipartFile> newImages) throws IOException {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("게시글 수정 권한이 없습니다.");
        }

        // 새 이미지 업로드
        List<String> imageUrls = post.getImageUrls();
        if (newImages != null && !newImages.isEmpty()) {
            List<String> newImageUrls = s3Service.uploadPostImages(newImages, userId);
            if (imageUrls == null) {
                imageUrls = newImageUrls;
            } else {
                imageUrls.addAll(newImageUrls);
            }
        }

        post.updatePost(request.getTitle(), request.getContent(), imageUrls);
        PostEntity updatedPost = postRepository.save(post);

        log.info("게시글 수정 완료: postId={}, userId={}", postId, userId);
        return convertToPostResponse(updatedPost);
    }

    /**
     * 게시글 삭제 (이미지도 함께 삭제)
     */
    public void deletePost(Long postId, Long userId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("게시글 삭제 권한이 없습니다.");
        }

        // S3에서 이미지 삭제
        if (post.getImageUrls() != null) {
            for (String imageUrl : post.getImageUrls()) {
                s3Service.deleteImage(imageUrl);
            }
        }

        postRepository.delete(post);
        log.info("게시글 삭제 완료: postId={}, userId={}", postId, userId);
    }

    // 나머지 메서드는 동일...

    public Page<PostListResponse> getAllPosts(Pageable pageable) {
        Page<PostEntity> posts = postRepository.findAllWithAuthor(pageable);
        return posts.map(this::convertToPostListResponse);
    }

    public PostResponse getPost(Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        post.incrementViewCount();
        postRepository.save(post);

        log.info("게시글 조회: postId={}, 조회수={}", postId, post.getViewCount());
        return convertToPostResponse(post);
    }

    public Page<PostListResponse> getMyPosts(Long userId, Pageable pageable) {
        Page<PostEntity> posts = postRepository.findByAuthorIdOrderByCreatedAtDesc(userId, pageable);
        return posts.map(this::convertToPostListResponse);
    }

    public Page<PostListResponse> searchPosts(String keyword, Pageable pageable) {
        Page<PostEntity> posts = postRepository.findByTitleContainingOrContentContaining(keyword, pageable);
        return posts.map(this::convertToPostListResponse);
    }

    private PostResponse convertToPostResponse(PostEntity post) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorNickname(post.getAuthor().getNickname())
                .authorId(post.getAuthor().getId())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .imageUrls(post.getImageUrls())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private PostListResponse convertToPostListResponse(PostEntity post) {
        String contentPreview = post.getContent().length() > 100
                ? post.getContent().substring(0, 100) + "..."
                : post.getContent();

        // 첫 번째 이미지를 썸네일로 사용
        String thumbnailUrl = null;
        if (post.getImageUrls() != null && !post.getImageUrls().isEmpty()) {
            thumbnailUrl = post.getImageUrls().get(0);
        }

        return PostListResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .contentPreview(contentPreview)
                .authorNickname(post.getAuthor().getNickname())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .thumbnailUrl(thumbnailUrl)
                .createdAt(post.getCreatedAt())
                .build();
    }
}