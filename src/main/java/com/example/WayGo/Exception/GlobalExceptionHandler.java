package com.example.WayGo.Exception;

import com.example.WayGo.Constant.ErrorCode;
import com.example.WayGo.Dto.Translation.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리 핸들러
 * 모든 Controller에서 발생하는 예외를 일관되게 처리
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Validation 실패 시 (@Valid 검증 실패)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "입력값이 올바르지 않습니다";

        log.warn("Validation failed: {}", message);
        return ApiResponse.error(ErrorCode.INVALID_REQUEST, message);
    }

    /**
     * 커스텀 번역 예외 처리
     */
    @ExceptionHandler(TranslationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleTranslationException(TranslationException e) {
        log.error("Translation error: {} - {}", e.getErrorCode().getCode(), e.getMessage(), e);
        return ApiResponse.error(e.getErrorCode(), e.getMessage());
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());
        return ApiResponse.error(ErrorCode.INVALID_REQUEST, e.getMessage());
    }

    /**
     * Google API 호출 실패 (네트워크 등)
     */
    @ExceptionHandler(com.google.api.gax.rpc.ApiException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<?> handleGoogleApiException(com.google.api.gax.rpc.ApiException e) {
        log.error("Google API error: {}", e.getMessage(), e);

        // API 타입별로 에러 코드 분기
        if (e.getStatusCode().getCode().getHttpStatusCode() == 401) {
            return ApiResponse.error(ErrorCode.API_KEY_INVALID);
        } else if (e.getStatusCode().getCode().getHttpStatusCode() == 429) {
            return ApiResponse.error(ErrorCode.API_LIMIT_EXCEEDED);
        }

        return ApiResponse.error(ErrorCode.TRANSLATION_FAILED, e.getMessage());
    }

    /**
     * 모든 예외의 최종 처리 (예상하지 못한 에러)
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<?> handleGeneralException(Exception e) {
        log.error("Unexpected error occurred: {}", e.getMessage(), e);
        return ApiResponse.error(ErrorCode.UNKNOWN_ERROR);
    }
}