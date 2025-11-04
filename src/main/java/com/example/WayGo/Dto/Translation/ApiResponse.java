package com.example.WayGo.Dto.Translation;

import com.example.WayGo.Constant.ErrorCode;  // ← import 추가
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * API 공통 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private String errorCode;

    /**
     * 성공 응답 생성
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data, null);
    }

    /**
     * 실패 응답 생성 (ErrorCode 사용)
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(
                false,
                errorCode.getMessage(),
                null,
                errorCode.getCode()
        );
    }

    /**
     * 실패 응답 생성 (커스텀 메시지)
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String customMessage) {
        return new ApiResponse<>(
                false,
                customMessage,
                null,
                errorCode.getCode()
        );
    }
}