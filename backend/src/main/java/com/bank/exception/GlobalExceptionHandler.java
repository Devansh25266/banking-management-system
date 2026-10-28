package com.bank.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BankException.class)
    ResponseEntity<Map<String, Object>> bank(BankException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("status", e.getStatus().value(), "message", e.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream().findFirst()
            .map(f -> f.getDefaultMessage()).orElse("Invalid request.");
        return ResponseEntity.badRequest().body(Map.of("status", 400, "message", msg));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Map<String, Object>> unreadable(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("status", 400, "message", "Malformed or invalid request."));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> other(Exception e) {
        if (e instanceof ErrorResponse er) {   // e.g. 404 unknown route, 405 wrong method
            int code = er.getStatusCode().value();
            return ResponseEntity.status(code).body(Map.of("status", code, "message", "Request could not be processed."));
        }
        log.error("Unhandled error", e);
        return ResponseEntity.status(500).body(Map.of("status", 500, "message", "Something went wrong. Please try again."));
    }
}
