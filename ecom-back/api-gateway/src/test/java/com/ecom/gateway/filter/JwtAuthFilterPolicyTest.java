package com.ecom.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

class JwtAuthFilterPolicyTest {

    @Test
    void shouldAllowPublicProductReadWithoutToken() {
        AuthValidationClient validationClient = org.mockito.Mockito.mock(AuthValidationClient.class);
        JwtAuthFilter filter = new JwtAuthFilter(
                validationClient,
                new GatewayAuthRoutePolicy(),
                new GatewayErrorWriter(new ObjectMapper()));
        TrackingChain chain = new TrackingChain();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.GET, "/api/products").build());

        filter.filter(exchange, chain).block();

        assertTrue(chain.called);
    }

    @Test
    void shouldRejectProtectedWriteWithoutToken() {
        AuthValidationClient validationClient = org.mockito.Mockito.mock(AuthValidationClient.class);
        JwtAuthFilter filter = new JwtAuthFilter(
                validationClient,
                new GatewayAuthRoutePolicy(),
                new GatewayErrorWriter(new ObjectMapper()));
        TrackingChain chain = new TrackingChain();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST, "/api/products").build());

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertTrue(!chain.called);
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
