package com.example.WayGo.Dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {
    private Long id;
    private String content;
    private String authorNickname;
    private Long authorId;
    private Long postId;
    private Long parentId; // 부모 댓글 ID (답글인 경우)
    private Boolean isDeleted;
    private Boolean isAuthor; // 현재 로그인한 사용자가 작성자인지
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<CommentResponse> replies = new ArrayList<>(); // 답글 목록
    private Integer replyCount; // 답글 개수
}
