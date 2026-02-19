import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Rate } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8084";
const TTL_MINUTES = Number(__ENV.TTL_MINUTES || "30");
const STOCK_QTY = Number(__ENV.STOCK_QTY || "10000");
const ITERATIONS = Number(__ENV.ITERATIONS || "10000");
const SUCCESS_TARGET = __ENV.RESERVE_SUCCESS_TARGET || "0.95";

const reserveSuccessRate = new Rate("reserve_success_rate");
const reserveServerErrorRate = new Rate("reserve_server_error_rate");
const oversellViolationRate = new Rate("oversell_violation_rate");
const reserveSuccessCount = new Counter("reserve_success_total");

export const options = {
  discardResponseBodies: true,
  scenarios: {
    flash_sale: {
      executor: "shared-iterations",
      vus: Number(__ENV.VUS || "200"),
      iterations: ITERATIONS,
      maxDuration: __ENV.MAX_DURATION || "2m",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<250"],
    reserve_success_rate: [`rate>${SUCCESS_TARGET}`],
    reserve_server_error_rate: ["rate<0.01"],
    oversell_violation_rate: ["rate==0"],
  },
};

export function setup() {
  const sku = `FLASH-SKU-${Date.now()}`;
  const upsertPayload = JSON.stringify({
    sku,
    availableQuantity: STOCK_QTY,
  });
  const headers = { "Content-Type": "application/json" };

  const response = http.post(`${BASE_URL}/api/inventory/stock`, upsertPayload, { headers });
  check(response, { "stock seeded": (r) => r.status === 201 });

  return { sku, stockQty: STOCK_QTY };
}

export default function (data) {
  const reservationId = `flash-${__VU}-${__ITER}`;
  const payload = JSON.stringify({
    reservationId,
    sku: data.sku,
    quantity: 1,
    ttlMinutes: TTL_MINUTES,
  });
  const headers = { "Content-Type": "application/json" };
  const response = http.post(`${BASE_URL}/api/inventory/reserve`, payload, { headers });

  const ok = response.status === 200;
  reserveSuccessRate.add(ok);
  reserveServerErrorRate.add(response.status >= 500);
  if (ok) {
    reserveSuccessCount.add(1);
  }

  check(response, {
    "reserve accepted or expected rejection": (r) => r.status === 200 || r.status === 400 || r.status === 409,
  });

  sleep(0.001);
}

export function teardown(data) {
  const response = http.get(`${BASE_URL}/api/inventory/stock/${data.sku}`);
  if (response.status !== 200) {
    oversellViolationRate.add(true);
    return;
  }

  const stock = response.json();
  const available = Number(stock.availableQuantity || 0);
  const reserved = Number(stock.reservedQuantity || 0);
  const violation =
    available < 0 ||
    reserved < 0 ||
    reserved > data.stockQty ||
    available + reserved !== data.stockQty;

  oversellViolationRate.add(violation);
}
