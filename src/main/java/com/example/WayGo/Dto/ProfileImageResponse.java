package com.example.WayGo.Dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileImageResponse {
    private boolean success;
    private String message;
    private String imageUrl;
    private String imageType; // "profile" or "background"
}