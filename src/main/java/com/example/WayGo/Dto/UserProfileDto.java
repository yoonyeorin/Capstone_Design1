package com.example.WayGo.Dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String loginId;
    private String nickname;
    private String name;
    private String email;
    private String profileImageUrl;
    private String backgroundImageUrl;
    private String role;
}