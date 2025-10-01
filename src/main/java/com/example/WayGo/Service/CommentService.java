package com.example.WayGo.Service;

import com.example.WayGo.Dto.CommentRequest;
import com.example.WayGo.Dto.CommentResponse;
import com.example.WayGo.Entity.CommentEntity;
import com.example.WayGo.Entity.PostEntity;
import com.example.WayGo.Entity.UserEntity;
import com.example.WayGo.Repository.CommentRepository;
import com.example.WayGo.Repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserService userService;

    /**
     * 댓글 작성
     */
    public CommentResponse createComment(Long userId, Long postId, CommentRequest request) {
        UserEntity author = userService.getLoginUserById(userId);
        if (author == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 부모 댓글 확인 (답글인 경우)
        CommentEntity parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("부모 댓글을 찾을 수 없습니다."));

            // 부모 댓글이 같은 게시글에 속하는지 확인
            if (!parent.getPost().getId().equals(postId)) {
                throw new RuntimeException("잘못된 요청입니다.");
            }

            // 대댓글의 대댓글은 불가 (2depth까지만)
            if (parent.getParent() != null) {
                throw new RuntimeException("답글에는 답글을 달 수 없습니다.");
            }
        }

        CommentEntity comment = CommentEntity.builder()
                .content(request.getContent())
                .post(post)
                .author(author)
                .parent(parent)
                .isDeleted(false)
                .build();

        CommentEntity savedComment = commentRepository.save(comment);

        // 게시글의 댓글 수 증가
        post.incrementCommentCount();
        postRepository.save(post);

        log.info("댓글 작성 완료: commentId={}, postId={}, userId={}, isReply={}",
                savedComment.getId(), postId, userId, parent != null);

        return convertToCommentResponse(savedComment, userId);
    }

    /**
     * 게시글의 댓글 목록 조회 (답글 포함)
     */
    public List<CommentResponse> getCommentsByPostId(Long postId, Long currentUserId) {
        // 부모 댓글만 조회
        List<CommentEntity> parentComments = commentRepository.findByPostIdAndParentIsNull(postId);

        return parentComments.stream()
                .map(comment -> {
                    CommentResponse response = convertToCommentResponse(comment, currentUserId);

                    // 답글 조회 및 추가
                    List<CommentEntity> replies = commentRepository.findByParentId(comment.getId());
                    List<CommentResponse> replyResponses = replies.stream()
                            .map(reply -> convertToCommentResponse(reply, currentUserId))
                            .collect(Collectors.toList());

                    response.setReplies(replyResponses);
                    response.setReplyCount(replyResponses.size());

                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * 댓글 수정
     */
    public CommentResponse updateComment(Long commentId, Long userId, CommentRequest request) {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("댓글 수정 권한이 없습니다.");
        }

        // 삭제된 댓글은 수정 불가
        if (comment.getIsDeleted()) {
            throw new RuntimeException("삭제된 댓글은 수정할 수 없습니다.");
        }

        comment.updateContent(request.getContent());
        CommentEntity updatedComment = commentRepository.save(comment);

        log.info("댓글 수정 완료: commentId={}, userId={}", commentId, userId);
        return convertToCommentResponse(updatedComment, userId);
    }

    /**
     * 댓글 삭제
     */
    public void deleteComment(Long commentId, Long userId) {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("댓글 삭제 권한이 없습니다.");
        }

        // 답글이 있는 경우 소프트 삭제, 없는 경우 하드 삭제
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            comment.delete();
            commentRepository.save(comment);
            log.info("댓글 소프트 삭제 (답글 존재): commentId={}, userId={}", commentId, userId);
        } else {
            // 게시글의 댓글 수 감소
            PostEntity post = comment.getPost();
            post.decrementCommentCount();
            postRepository.save(post);

            commentRepository.delete(comment);
            log.info("댓글 하드 삭제: commentId={}, userId={}", commentId, userId);
        }
    }

    /**
     * 사용자가 작성한 댓글 조회
     */
    public Page<CommentResponse> getMyComments(Long userId, Pageable pageable) {
        Page<CommentEntity> comments = commentRepository.findByAuthorIdOrderByCreatedAtDesc(userId, pageable);
        return comments.map(comment -> convertToCommentResponse(comment, userId));
    }

    /**
     * Entity를 DTO로 변환
     */
    private CommentResponse convertToCommentResponse(CommentEntity comment, Long currentUserId) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorNickname(comment.getAuthor().getNickname())
                .authorId(comment.getAuthor().getId())
                .postId(comment.getPost().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .isDeleted(comment.getIsDeleted())
                .isAuthor(currentUserId != null && comment.getAuthor().getId().equals(currentUserId))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replies(null) // 나중에 설정
                .replyCount(0)
                .build();
    }
}
