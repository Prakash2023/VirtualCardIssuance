package com.example.virtualCard.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

}
