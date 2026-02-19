# Payment Outage Drill Runbook

## Goal
Validate `payment-service` outage-retry-DLQ alerting path and receiver routing.

## Prerequisites
- Infra stack is running (`prometheus`, `alertmanager`).
- `payment-service` is running with metrics enabled.
- Alertmanager env has payment receiver vars:
  - `ALERTMANAGER_SLACK_CHANNEL_PAYMENT`
  - `ALERTMANAGER_PD_ROUTING_KEY_PAYMENT`
  - `ALERTMANAGER_EMAIL_PAYMENT`

## Drill Steps
1. Enable provider outage mode:
   - `POST /api/payments/provider/outage-mode?enabled=true`
2. Trigger provider retries and DLQ entries:
   - Send multiple `POST /api/payments/intents` requests with unique `idempotencyKey`.
   - Expect failures: `Payment provider unavailable; request moved to DLQ`.
3. Verify Prometheus metrics:
   - `increase(payment_provider_retry_total[10m])`
   - `increase(payment_provider_dlq_total[10m])`
   - `increase(payment_provider_requeue_total{result="failed"}[15m])`
4. Verify alert firing in Prometheus:
   - `PaymentProviderRetrySpike`
   - `PaymentProviderDlqIncrease`
   - `PaymentProviderRequeueFailures` (if requeue failure injected)
5. Verify Alertmanager routing:
   - Warning alerts -> `team-payment-warning`
   - Critical alerts -> `team-payment-critical`
6. Recover and requeue:
   - `POST /api/payments/provider/outage-mode?enabled=false`
   - `GET /api/payments/provider/dead-letters`
   - `POST /api/payments/provider/dead-letters/{id}/requeue`
7. Confirm alerts resolve after recovery window.

## Expected Outcome
- Retry spikes create warning alerts.
- DLQ creation creates critical alerts.
- Alerts reach payment owner receivers and resolve after recovery.
