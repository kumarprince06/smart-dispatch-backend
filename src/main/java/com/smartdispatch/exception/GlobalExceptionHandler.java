package com.smartdispatch.exception;

import com.smartdispatch.common.dto.ApiResponse;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle custom BadRequestException
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
            BadRequestException ex
    ) {
        return ResponseEntity.badRequest().body(
                ApiResponse.<Void>builder()
                        .success(false)
                        .message(ex.getMessage())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .build()
        );
    }

    // Handle validation errors (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");

        return ResponseEntity.badRequest().body(
                ApiResponse.<Void>builder()
                        .success(false)
                        .message(message)
                        .status(HttpStatus.BAD_REQUEST.value())
                        .build()
        );
    }

    // Handle bad credentials (login failures)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException ex
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.<Void>builder()
                        .success(false)
                        .message(ex.getMessage())
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .build()
        );
    }

    // Handle user not found
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(
            UsernameNotFoundException ex
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.<Void>builder()
                        .success(false)
                        .message(ex.getMessage())
                        .status(HttpStatus.NOT_FOUND.value())
                        .build()
        );
    }

    // Handle RuntimeException
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(
            RuntimeException ex
    ) {
        return ResponseEntity.badRequest().body(
                ApiResponse.<Void>builder()
                        .success(false)
                        .message(ex.getMessage())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .build()
        );
    }

    // Handle all other unhandled exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(
            Exception ex
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Void>builder()
                        .success(false)
                        .message("Something went wrong. Please try again later.")
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .build()
        );
    }
}
