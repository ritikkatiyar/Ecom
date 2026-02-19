package com.ecom.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class ReleaseGateMetricsServiceTest {

    @Test
    void shouldIncrementTotalAndFailureCountersForFailureStatus() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ReleaseGateMetricsService service = new ReleaseGateMetricsService(meterRegistry);

        service.recordCallback("rollback_verification", "staging", "failed");

        double total = meterRegistry.get("release_gate_rollback_callback_total")
                .tag("event", "rollback_verification")
                .tag("environment", "staging")
                .tag("status", "failed")
                .counter()
                .count();

        double failure = meterRegistry.get("release_gate_rollback_callback_failure_total")
                .tag("event", "rollback_verification")
                .tag("environment", "staging")
                .counter()
                .count();

        assertThat(total).isEqualTo(1.0);
        assertThat(failure).isEqualTo(1.0);
    }

    @Test
    void shouldIncrementOnlyTotalCounterForNonFailureStatus() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ReleaseGateMetricsService service = new ReleaseGateMetricsService(meterRegistry);

        service.recordCallback("rollback_verification", "production", "triggered");

        double total = meterRegistry.get("release_gate_rollback_callback_total")
                .tag("event", "rollback_verification")
                .tag("environment", "production")
                .tag("status", "triggered")
                .counter()
                .count();

        assertThat(total).isEqualTo(1.0);
        assertThat(meterRegistry.find("release_gate_rollback_callback_failure_total").counter()).isNull();
    }
}
