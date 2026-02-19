# Alert Drill Runbook

## Goal
Validate Prometheus -> Alertmanager routing for warning and critical alerts.

## Prerequisites
- Infra stack running from `ecom-back/infrastructure/docker-compose.yml`.
- Prometheus loaded with `alerts.yml`.
- Alertmanager loaded with `alertmanager.yml`.
- `alertmanager.env` present (copy from `alertmanager.env.example` and set real values).
- Optional local audit webhook listener on port `18080`.

## Drill Steps
1. Start observability stack:
   - `docker compose -f ecom-back/infrastructure/docker-compose.yml up -d prometheus alertmanager grafana`
2. Trigger warning path:
   - force `NotificationDeadLetterSpike` by sending test events into notification dead-letter flow.
   - verify routing to notification warning receiver (`team-notification-warning`).
3. Trigger critical path:
   - force `OrderOutboxFailuresPresent` by setting at least one failed order outbox record.
   - verify routing to order critical receiver (`team-order-critical`).
4. Verify in Prometheus:
   - UI: `http://localhost:9090/alerts`
   - confirm target alerts move to `firing`.
5. Verify in Alertmanager:
   - UI: `http://localhost:9093`
   - confirm grouped route and receiver selection.
6. Verify receiver delivery:
   - Slack: message appears in configured channel.
   - PagerDuty: incident created for critical route.
   - Email: mail delivered for team route.
   - Audit webhook: payload posted to `${ALERTMANAGER_AUDIT_WEBHOOK_URL}`.

## Expected Outcome
- Warning alerts route to service-owner warning receiver.
- Critical alerts route to service-owner critical receiver (and/or platform critical fallback).
- Resolved notifications are delivered when alert state returns to normal.

## Follow-up
- Keep `OWNERSHIP_MAP.md` updated for every new service alert.
- Re-run this drill after receiver secret rotation.
- For payment provider outage-specific validation, run `PAYMENT_OUTAGE_DRILL.md`.
