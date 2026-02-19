package com.ecom.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

class ApiVersionFilterTest {

    @Test
    void shouldRejectMissingApiVersionForProtectedApi() {
        GatewayErrorWriter writer = new GatewayErrorWriter(new ObjectMapper());
        ApiVersionFilter filter = new ApiVersionFilter("v1", writer);
        TrackingChain chain = new TrackingChain();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products").build());

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
        String body = ((MockServerHttpResponse) exchange.getResponse()).getBodyAsString().block();
        assertTrue(body != null && body.contains("API_VERSION_MISMATCH"));
        assertTrue(!chain.called);
    }

    @Test
    void shouldSkipVersionGuardForAuthRoutes() {
        GatewayErrorWriter writer = new GatewayErrorWriter(new ObjectMapper());
        ApiVersionFilter filter = new ApiVersionFilter("v1", writer);
        TrackingChain chain = new TrackingChain();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/login").build());

        filter.filter(exchange, chain).block();

        assertTrue(chain.called);
    }

    private static class TrackingChain implements GatewayFilterChain {
        private boolean called;

        @Override
        public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange) {
            called = true;
            return Mono.empty();
        }
    }
}
