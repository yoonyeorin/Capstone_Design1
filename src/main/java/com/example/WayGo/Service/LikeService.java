package com.example.WayGo.Service;

import com.example.WayGo.Dto.LikedPostResponse;
import com.example.WayGo.Entity.LikeEntity;
import com.example.WayGo.Entity.PostEntity;
import com.example.WayGo.Entity.UserEntity;
import com.example.WayGo.Repository.LikeRepository;
import com.example.WayGo.Repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final UserService userService;

    /**
     * 좋아요 추가
     */
    public boolean addLike(Long userId, Long postId) {
        // 이미 좋아요가 있는지 확인
        if (likeRepository.existsByUserIdAndPostId(userId, postId)) {
            log.warn("이미 좋아요가 존재: userId={}, postId={}", userId, postId);
            return false;
        }

        UserEntity user = userService.getLoginUserById(userId);
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 좋아요 생성
        LikeEntity like = LikeEntity.builder()
                .user(user)
                .post(post)
                .build();

        likeRepository.save(like);

        // 게시글의 좋아요 수 증가
        post.incrementLikeCount();
        postRepository.save(post);

        log.info("좋아요 추가 성공: userId={}, postId={}", userId, postId);
        return true;
    }

    /**
     * 좋아요 취소
     */
    public boolean removeLike(Long userId, Long postId) {
        Optional<LikeEntity> like = likeRepository.findByUserIdAndPostId(userId, postId);

        if (like.isEmpty()) {
            log.warn("좋아요가 존재하지 않음: userId={}, postId={}", userId, postId);
            return false;
        }

        // 좋아요 삭제
        likeRepository.delete(like.get());

        // 게시글의 좋아요 수 감소
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        post.decrementLikeCount();
        postRepository.save(post);

        log.info("좋아요 취소 성공: userId={}, postId={}", userId, postId);
        return true;
    }

    /**
     * 좋아요 상태 확인
     */
    public boolean isLiked(Long userId, Long postId) {
        return likeRepository.existsByUserIdAndPostId(userId, postId);
    }

    /**
     * 게시글 좋아요 수 조회
     */
    public long getLikeCount(Long postId) {
        return likeRepository.countByPostId(postId);
    }

    /**
     * 사용자가 좋아요한 게시글 목록 조회 (마이페이지용)
     */
    public Page<LikedPostResponse> getUserLikedPosts(Long userId, Pageable pageable) {
        Page<LikeEntity> likes = likeRepository.findByUserIdWithPost(userId, pageable);
        return likes.map(this::convertToLikedPostResponse);
    }

    // DTO 변환 메서드
    private LikedPostResponse convertToLikedPostResponse(LikeEntity like) {
        PostEntity post = like.getPost();

        // 내용 미리보기 (100자로 제한)
        String contentPreview = post.getContent().length() > 100
                ? post.getContent().substring(0, 100) + "..."
                : post.getContent();

        return LikedPostResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .contentPreview(contentPreview)
                .authorNickname(post.getAuthor().getNickname())
                .authorId(post.getAuthor().getId())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .postCreatedAt(post.getCreatedAt())
                .likedAt(like.getCreatedAt())
                .build();
    }
}
