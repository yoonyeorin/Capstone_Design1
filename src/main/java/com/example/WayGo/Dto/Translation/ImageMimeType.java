package com.example.WayGo.Dto.Translation;

/**
 * 이미지 MIME 타입 (OCR 입력용)
 */
public enum ImageMimeType {
    IMAGE_PNG("image/png"),
    IMAGE_JPEG("image/jpeg"),
    IMAGE_WEBP("image/webp"),
    IMAGE_TIFF("image/tiff");

    private final String mime;

    ImageMimeType(String mime) {
        this.mime = mime;
    }

    public String getMime() {
        return mime;
    }
}
