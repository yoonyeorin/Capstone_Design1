package com.example.WayGo.Repository;

import com.example.WayGo.Entity.LikeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<LikeEntity, Long> {

    // 특정 사용자가 특정 게시글에 좋아요를 눌렀는지 확인
    boolean existsByUserIdAndPostId(Long userId, Long postId);

    // 특정 사용자가 특정 게시글에 누른 좋아요 찾기
    Optional<LikeEntity> findByUserIdAndPostId(Long userId, Long postId);

    // 특정 게시글의 좋아요 수 계산
    long countByPostId(Long postId);

    // 사용자가 좋아요한 게시글 목록 (게시글 정보 포함)
    @Query("SELECT l FROM LikeEntity l JOIN FETCH l.post p JOIN FETCH p.author WHERE l.user.id = :userId ORDER BY p.createdAt DESC")
    Page<LikeEntity> findByUserIdWithPost(@Param("userId") Long userId, Pageable pageable);
}
