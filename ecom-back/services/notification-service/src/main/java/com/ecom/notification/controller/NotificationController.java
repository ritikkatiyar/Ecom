package com.ecom.notification.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecom.notification.dto.NotificationDeadLetterResponse;
import com.ecom.notification.dto.NotificationResponse;
import com.ecom.notification.service.NotificationUseCases;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationUseCases notificationService;

    public NotificationController(NotificationUseCases notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> listByUser(@RequestParam Long userId) {
        return notificationService.listByUser(userId);
    }

    @GetMapping("/failed")
    public List<NotificationResponse> failed() {
        return notificationService.listFailed();
    }

    @PostMapping("/retry-failed")
    public ResponseEntity<String> retryFailed() {
        int retried = notificationService.retryFailed();
        return ResponseEntity.ok("Retried notifications: " + retried);
    }

    @GetMapping("/dead-letters")
    public List<NotificationDeadLetterResponse> deadLetters() {
        return notificationService.listDeadLetters();
    }

    @PostMapping("/dead-letters/{id}/requeue")
    public ResponseEntity<String> requeueDeadLetter(@PathVariable Long id) {
        boolean requeued = notificationService.requeueDeadLetter(id);
        if (!requeued) {
            return ResponseEntity.badRequest().body("Dead letter not found or already requeued");
        }
        return ResponseEntity.ok("Dead letter requeued");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
