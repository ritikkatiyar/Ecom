# Alert Ownership Map

## Services to Owners
- `order-service`
  - Primary team: Order Platform
  - Slack: `${ALERTMANAGER_SLACK_CHANNEL_ORDER}`
  - PagerDuty key: `${ALERTMANAGER_PD_ROUTING_KEY_ORDER}`
  - Email: `${ALERTMANAGER_EMAIL_ORDER}`
- `notification-service`
  - Primary team: Notification Platform
  - Slack: `${ALERTMANAGER_SLACK_CHANNEL_NOTIFICATION}`
  - PagerDuty key: `${ALERTMANAGER_PD_ROUTING_KEY_NOTIFICATION}`
  - Email: `${ALERTMANAGER_EMAIL_NOTIFICATION}`
- `payment-service`
  - Primary team: Payment Platform
  - Slack: `${ALERTMANAGER_SLACK_CHANNEL_PAYMENT}`
  - PagerDuty key: `${ALERTMANAGER_PD_ROUTING_KEY_PAYMENT}`
  - Email: `${ALERTMANAGER_EMAIL_PAYMENT}`
- `*` (critical fallback)
  - Primary team: Platform Reliability
  - Slack: `${ALERTMANAGER_SLACK_CHANNEL_CRITICAL}`
  - PagerDuty key: `${ALERTMANAGER_PD_ROUTING_KEY_CRITICAL}`

## Routing Rules
1. Alerts with `service=order-service` route to order receivers (warning/critical split by `severity`).
2. Alerts with `service=notification-service` route to notification receivers (warning/critical split).
3. Alerts with `service=payment-service` route to payment receivers (warning/critical split).
4. Any remaining `severity=critical` alert routes to platform critical receiver.
5. All alerts route through `platform-default` audit webhook for traceability.

## Operational Notes
- Create `ecom-back/infrastructure/alertmanager/alertmanager.env` from `alertmanager.env.example`.
- Keep secrets out of git; set real values in environment-specific secret stores.
- Update this file whenever new service alerts are introduced.
