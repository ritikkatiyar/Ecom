package com.ecom.gateway.controller;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.ecom.gateway.filter.CorrelationIdFilter;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/{service}")
    public ResponseEntity<Map<String, Object>> serviceFallback(
            @PathVariable("service") String service,
            ServerWebExchange exchange) {
        Throwable cause = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", Instant.now().toString());
        payload.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        payload.put("error", HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase());
        payload.put("code", "DOWNSTREAM_UNAVAILABLE");
        payload.put("message", "Upstream service '" + service + "' is temporarily unavailable.");
        payload.put("service", service);
        payload.put("path", exchange.getRequest().getPath().value());
        payload.put("correlationId", exchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER));
        if (cause != null) {
            payload.put("cause", cause.getClass().getSimpleName());
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(payload);
    }
}
