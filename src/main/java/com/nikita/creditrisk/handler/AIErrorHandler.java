package com.nikita.creditrisk.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * SPRING AI - ERROR HANDLING (Global Exception Handler)
 * 
 * Handles all exceptions from the Credit Risk service including:
 * 1. AI Service unavailable (API timeout, rate limiting)
 * 2. Invalid customer ID (data not found)
 * 3. Document ingestion failures
 * 4. General runtime exceptions
 * 
 * Works in conjunction with:
 * - @Retryable (Spring Retry) for automatic retries with backoff
 * - FallbackService (@Recover) for graceful degradation
 * - This handler catches any remaining unhandled exceptions
 */
@RestControllerAdvice
public class AIErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(AIErrorHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
        log.warn("⚠ Bad request: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "error", "Bad Request",
            "message", e.getMessage(),
            "timestamp", LocalDateTime.now().toString(),
            "suggestion", "Please check the customer ID and try again. Valid IDs: CUST001, CUST002, CUST003, CUST004"
        ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        log.error("❌ Runtime error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "error", "Service Error",
            "message", "An unexpected error occurred: " + e.getMessage(),
            "timestamp", LocalDateTime.now().toString(),
            "fallbackAvailable", true,
            "suggestion", "The system will attempt to provide a rule-based fallback response."
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("❌ Unhandled exception: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "error", "Internal Server Error",
            "message", "Service temporarily unavailable. Fallback responses are being served.",
            "timestamp", LocalDateTime.now().toString(),
            "fallbackAvailable", true
        ));
    }
}
