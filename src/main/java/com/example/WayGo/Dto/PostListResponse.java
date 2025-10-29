package com.example.WayGo.Dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostListResponse {
    private Long id;
    private String title;
    private String contentPreview;
    private String authorNickname;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private String thumbnailUrl; // 첫 번째 이미지를 썸네일로 사용
    private LocalDateTime createdAt;
}