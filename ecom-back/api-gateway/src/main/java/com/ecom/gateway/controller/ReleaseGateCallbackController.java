package com.ecom.gateway.controller;

import com.ecom.gateway.service.ReleaseGateMetricsService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/release-gate")
public class ReleaseGateCallbackController {

    private final ReleaseGateMetricsService releaseGateMetricsService;

    public ReleaseGateCallbackController(ReleaseGateMetricsService releaseGateMetricsService) {
        this.releaseGateMetricsService = releaseGateMetricsService;
    }

    @PostMapping("/callbacks")
    public ResponseEntity<Map<String, Object>> recordCallback(@RequestBody ReleaseGateCallbackRequest request) {
        if (isBlank(request.event()) || isBlank(request.environment()) || isBlank(request.status())) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("timestamp", Instant.now().toString());
            payload.put("status", HttpStatus.BAD_REQUEST.value());
            payload.put("error", "event, environment, and status are required");
            return ResponseEntity.badRequest().body(payload);
        }

        releaseGateMetricsService.recordCallback(request.event(), request.environment(), request.status());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", Instant.now().toString());
        payload.put("status", "accepted");
        payload.put("event", request.event());
        payload.put("environment", request.environment());
        payload.put("callbackStatus", request.status());
        return ResponseEntity.accepted().body(payload);
    }

    public record ReleaseGateCallbackRequest(
            String event,
            String environment,
            String status,
            String ref,
            String runId,
            String details) {
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
