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

    /**
     * Lists notifications for a given user.
     */
    @GetMapping
    public List<NotificationResponse> listByUser(@RequestParam(name = "userId") Long userId) {
        return notificationService.listByUser(userId);
    }

    /**
     * Lists notifications that failed delivery but have not been reprocessed yet.
     */
    @GetMapping("/failed")
    public List<NotificationResponse> failed() {
        return notificationService.listFailed();
    }

    /**
     * Retries all notifications currently marked as failed.
     */
    @PostMapping("/retry-failed")
    public ResponseEntity<String> retryFailed() {
        int retried = notificationService.retryFailed();
        return ResponseEntity.ok("Retried notifications: " + retried);
    }

    /**
     * Returns dead-lettered notification events for inspection.
     */
    @GetMapping("/dead-letters")
    public List<NotificationDeadLetterResponse> deadLetters() {
        return notificationService.listDeadLetters();
    }

    /**
     * Requeues a dead-lettered notification event.
     */
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
