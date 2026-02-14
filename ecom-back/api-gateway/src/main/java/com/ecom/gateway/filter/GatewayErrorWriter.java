package com.ecom.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Component
public class GatewayErrorWriter {

    private final ObjectMapper objectMapper;

    public GatewayErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(
            ServerWebExchange exchange,
            HttpStatus status,
            String code,
            String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", Instant.now().toString());
        payload.put("status", status.value());
        payload.put("error", status.getReasonPhrase());
        payload.put("code", code);
        payload.put("message", message);
        payload.put("path", exchange.getRequest().getPath().value());
        payload.put("correlationId", exchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER));

        byte[] body = toJson(payload);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    private byte[] toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException ex) {
            String fallback = "{\"code\":\"GATEWAY_SERIALIZATION_ERROR\",\"message\":\"Failed to build error response\"}";
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }
}
