package com.ecom.product.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
@ConditionalOnBean(com.cloudinary.Cloudinary.class)
public class ImageUploadService {

    private static final List<String> ALLOWED_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5MB

    private final Cloudinary cloudinary;

    public ImageUploadService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String upload(MultipartFile file) throws IOException {
        validate(file);
        Map<?, ?> options = ObjectUtils.asMap(
                "folder", "ecom/products",
                "use_filename", true,
                "unique_filename", true);
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);
        String url = (String) result.get("secure_url");
        if (url == null) {
            url = (String) result.get("url");
        }
        return url;
    }

    public List<String> uploadMultiple(MultipartFile[] files) {
        return Arrays.stream(files)
                .filter(f -> f != null && !f.isEmpty())
                .map(f -> {
                    try {
                        return upload(f);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to upload " + f.getOriginalFilename(), e);
                    }
                })
                .collect(Collectors.toList());
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Invalid image type. Allowed: JPEG, PNG, WebP, GIF");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 5MB");
        }
    }
}
