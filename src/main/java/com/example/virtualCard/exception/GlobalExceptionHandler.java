package com.example.virtualCard.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler  {
    @ExceptionHandler(CardNotActiveException.class)
    public ResponseEntity<ApiErrorResponse>handleNotFound(CardNotActiveException ex){
        return new ResponseEntity<>(new ApiErrorResponse(400,ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(CardNotFoundException ex) {
        return new ResponseEntity<>(
                new ApiErrorResponse(404, ex.getMessage()),
                HttpStatus.NOT_FOUND
        );
    }
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiErrorResponse> handleBalance(InsufficientBalanceException ex) {
        return new ResponseEntity<>(
                new ApiErrorResponse(400, ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleIdempotencyConflict(IdempotencyConflictException ex) {
        return new ResponseEntity<>(
                new ApiErrorResponse(409, ex.getMessage()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(IdempotencyInProgressException.class)
    public ResponseEntity<ApiErrorResponse> handleIdempotencyInProgress(IdempotencyInProgressException ex) {
        return new ResponseEntity<>(
                new ApiErrorResponse(409, ex.getMessage()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Invalid request");
        return new ResponseEntity<>(
                new ApiErrorResponse(400, message),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return new ResponseEntity<>(
                new ApiErrorResponse(400, ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }
}
