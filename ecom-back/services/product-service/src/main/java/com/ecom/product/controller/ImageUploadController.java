package com.ecom.product.controller;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.product.service.ImageUploadService;

@RestController
@RequestMapping("/api/products")
@ConditionalOnExpression(
        "T(org.springframework.util.StringUtils).hasText('${cloudinary.cloud-name:}') and " +
        "T(org.springframework.util.StringUtils).hasText('${cloudinary.api-key:}') and " +
        "T(org.springframework.util.StringUtils).hasText('${cloudinary.api-secret:}')")
public class ImageUploadController {

    private static final Logger log = LoggerFactory.getLogger(ImageUploadController.class);

    private final ImageUploadService imageUploadService;

    public ImageUploadController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    /**
     * Uploads one or more product images and returns their accessible URLs.
     */
    @PostMapping("/images")
    public ResponseEntity<List<String>> uploadImages(@RequestParam("files") MultipartFile[] files) {
        log.info("uploadImages invoked fileCount={}", files != null ? files.length : 0);
        List<String> urls = imageUploadService.uploadMultiple(files);
        log.info("uploadImages completed urlCount={}", urls != null ? urls.size() : 0);
        return ResponseEntity.ok(urls);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<String> handleIOException(IOException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ex.getMessage() != null ? ex.getMessage() : "Image upload failed");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntime(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }
}
