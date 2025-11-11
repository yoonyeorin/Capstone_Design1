package com.example.WayGo.Exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("Validation 오류: {}", errors);
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(InputNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleInputNotFoundException(
            InputNotFoundException ex) {

        Map<String, String> error = new HashMap<>();
        error.put("error", "INPUT_NOT_FOUND");
        error.put("message", ex.getMessage());

        log.error("입력 조회 실패: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DateOverlapException.class)
    public ResponseEntity<Map<String, String>> handleDateOverlapException(
            DateOverlapException ex) {

        Map<String, String> error = new HashMap<>();
        error.put("error", "DATE_OVERLAP");
        error.put("message", ex.getMessage());

        log.error("날짜 겹침: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(
            Exception ex) {

        Map<String, String> error = new HashMap<>();
        error.put("error", "INTERNAL_SERVER_ERROR");
        error.put("message", "서버 오류가 발생했습니다");

        log.error("예상치 못한 오류", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}