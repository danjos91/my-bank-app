package io.github.danjos.mybankapp.accounts.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        List<String> errorMessages = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> {
                    String fieldName = error.getField();
                    String errorMessage = error.getDefaultMessage();
                    // Translate field names to user-friendly messages
                    String translatedField = translateFieldName(fieldName);
                    return translatedField + ": " + errorMessage;
                })
                .collect(Collectors.toList());
        
        errors.put("error", "Validation failed");
        errors.put("errors", errorMessages);
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("error", e.getMessage());
        response.put("errors", List.of(e.getMessage()));
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Internal server error: " + e.getMessage());
        response.put("errors", List.of("Internal server error: " + e.getMessage()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String translateFieldName(String fieldName) {
        Map<String, String> translations = Map.of(
            "username", "Логин",
            "password", "Пароль",
            "firstName", "Имя",
            "lastName", "Фамилия",
            "email", "Email",
            "birthDate", "Дата рождения"
        );
        return translations.getOrDefault(fieldName, fieldName);
    }
}


