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
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private String authorNickname;
    private Long authorId;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private List<String> imageUrls; // 이미지 URL 리스트 추가
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}