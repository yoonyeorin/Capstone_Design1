package com.example.WayGo.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class CommentRequest {

    @NotBlank(message = "댓글 내용이 비어있습니다.")
    @Size(min = 1, max = 1000, message = "댓글은 1자 이상 1000자 이하로 입력해주세요.")
    private String content;

    private Long parentId; // 답글인 경우 부모 댓글 ID
}
