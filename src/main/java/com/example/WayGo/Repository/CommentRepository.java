package com.example.WayGo.Repository;

import com.example.WayGo.Entity.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    // 특정 게시글의 댓글만 조회 (대댓글 제외)
    @Query("SELECT c FROM CommentEntity c JOIN FETCH c.author WHERE c.post.id = :postId AND c.parent IS NULL ORDER BY c.createdAt ASC")
    List<CommentEntity> findByPostIdAndParentIsNull(@Param("postId") Long postId);

    // 특정 게시글의 모든 댓글 수 (삭제된 것 제외)
    long countByPostIdAndIsDeletedFalse(Long postId);

    // 특정 댓글의 답글 조회
    @Query("SELECT c FROM CommentEntity c JOIN FETCH c.author WHERE c.parent.id = :parentId ORDER BY c.createdAt ASC")
    List<CommentEntity> findByParentId(@Param("parentId") Long parentId);

    // 사용자가 작성한 댓글 조회
    Page<CommentEntity> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);
}
