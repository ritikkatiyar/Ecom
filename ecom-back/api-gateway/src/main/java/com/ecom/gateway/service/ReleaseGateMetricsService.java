package com.ecom.gateway.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class ReleaseGateMetricsService {

    private final MeterRegistry meterRegistry;

    public ReleaseGateMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordCallback(String event, String environment, String status) {
        meterRegistry.counter(
                "release_gate_rollback_callback_total",
                "event", event,
                "environment", environment,
                "status", status)
                .increment();

        if (isFailure(status)) {
            meterRegistry.counter(
                    "release_gate_rollback_callback_failure_total",
                    "event", event,
                    "environment", environment)
                    .increment();
        }
    }

    private boolean isFailure(String status) {
        String normalized = status.toLowerCase();
        return normalized.contains("fail")
                || normalized.contains("error")
                || normalized.contains("timeout");
    }
}
