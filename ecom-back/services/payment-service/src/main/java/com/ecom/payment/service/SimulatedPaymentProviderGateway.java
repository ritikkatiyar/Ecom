package com.ecom.payment.service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SimulatedPaymentProviderGateway implements PaymentProviderGateway {

    private final AtomicBoolean outageMode;
    private final double failureRate;

    public SimulatedPaymentProviderGateway(
            @Value("${app.payment.provider.outage-mode:false}") boolean outageMode,
            @Value("${app.payment.provider.failure-rate:0.0}") double failureRate) {
        this.outageMode = new AtomicBoolean(outageMode);
        this.failureRate = Math.max(0.0, Math.min(1.0, failureRate));
    }

    @Override
    public String createPaymentId(String orderId, BigDecimal amount, String currency) {
        if (outageMode.get()) {
            throw new IllegalStateException("Payment provider unavailable (outage mode)");
        }
        if (failureRate > 0.0 && ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new IllegalStateException("Payment provider transient failure");
        }
        return "rzp_" + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public void setOutageMode(boolean enabled) {
        outageMode.set(enabled);
    }

    @Override
    public boolean isOutageMode() {
        return outageMode.get();
    }
}
