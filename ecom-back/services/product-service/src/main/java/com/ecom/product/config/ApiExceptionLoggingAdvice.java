package com.ecom.product.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Logs 400-causing exceptions for image upload and other API errors.
 */
@RestControllerAdvice
public class ApiExceptionLoggingAdvice {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionLoggingAdvice.class);

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<String> handleMultipart(MultipartException ex, HttpServletRequest req) {
        log.warn("api 400 multipart error path={} error={}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<String> handleMissingParam(
            org.springframework.web.bind.MissingServletRequestParameterException ex,
            HttpServletRequest req) {
        log.warn("api 400 missing param path={} param={} error={}",
                req.getRequestURI(), ex.getParameterName(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing required parameter: " + ex.getParameterName());
    }
}
