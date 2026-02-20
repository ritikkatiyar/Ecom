package com.ecom.gateway.filter;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
public class GatewayAuthRoutePolicy {

    public boolean isProtected(String path, HttpMethod method) {
        if (path.startsWith("/api/cart")
                || path.startsWith("/api/orders")
                || path.startsWith("/api/payments")
                || path.startsWith("/api/inventory")
                || path.startsWith("/api/users")
                || path.startsWith("/api/reviews")) {
            return true;
        }

        if (path.startsWith("/api/products")) {
            return method != HttpMethod.GET;
        }

        if (path.startsWith("/api/search")) {
            return method != HttpMethod.GET;
        }

        return false;
    }
}
