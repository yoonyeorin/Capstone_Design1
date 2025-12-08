package com.example.WayGo.Exception;

import com.example.WayGo.Constant.ErrorCode;
import lombok.Getter;

/**
 * 일정 서비스 커스텀 예외
 */
@Getter
public class ItineraryException extends RuntimeException {

    private final ErrorCode errorCode;

    public ItineraryException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ItineraryException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public ItineraryException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ItineraryException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
    }
}