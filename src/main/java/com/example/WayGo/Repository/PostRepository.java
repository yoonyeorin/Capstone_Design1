package com.example.WayGo.Repository;

import com.example.WayGo.Entity.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<PostEntity, Long> {

    // 게시글 목록 조회 (작성자 정보 포함)
    @Query("SELECT p FROM PostEntity p JOIN FETCH p.author ORDER BY p.createdAt DESC")
    Page<PostEntity> findAllWithAuthor(Pageable pageable);

    // 특정 사용자의 게시글 조회
    Page<PostEntity> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    // 제목이나 내용으로 검색
    @Query("SELECT p FROM PostEntity p JOIN FETCH p.author WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword% ORDER BY p.createdAt DESC")
    Page<PostEntity> findByTitleContainingOrContentContaining(@Param("keyword") String keyword, Pageable pageable);
}
