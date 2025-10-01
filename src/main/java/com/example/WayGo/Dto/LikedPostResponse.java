package com.example.WayGo.Dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikedPostResponse {
    private Long postId;
    private String title;
    private String contentPreview;
    private String authorNickname;
    private Long authorId;
    private Long likeCount;
    private Long commentCount;
    private Long viewCount;
    private LocalDateTime postCreatedAt;
    private LocalDateTime likedAt; // 좋아요 누른 시간
}
