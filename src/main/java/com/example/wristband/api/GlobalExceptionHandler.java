package com.example.wristband.api;

import com.example.wristband.service.PrinterUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<PrintJobResponse> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new PrintJobResponse("ERROR", "Validation failed: " + details));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<PrintJobResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new PrintJobResponse("ERROR", ex.getMessage()));
    }

    @ExceptionHandler(PrinterUnavailableException.class)
    public ResponseEntity<PrintJobResponse> handlePrinterUnavailable(PrinterUnavailableException ex) {
        logger.error("Printer unavailable: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new PrintJobResponse("ERROR", "Printer unavailable"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<PrintJobResponse> handleGeneric(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new PrintJobResponse("ERROR", "Internal server error"));
    }
}
