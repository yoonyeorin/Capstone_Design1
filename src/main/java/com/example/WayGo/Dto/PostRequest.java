package com.example.WayGo.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class PostRequest {

    @NotBlank(message = "제목이 비어있습니다.")
    @Size(min = 1, max = 200, message = "제목은 1자 이상 200자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "내용이 비어있습니다.")
    @Size(min = 1, max = 10000, message = "내용은 1자 이상 10000자 이하로 입력해주세요.")
    private String content;
}
