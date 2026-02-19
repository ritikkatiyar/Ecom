import http from "k6/http";
import { check, sleep } from "k6";
import { Rate } from "k6/metrics";

const ORDER_BASE_URL = __ENV.ORDER_BASE_URL || "http://localhost:8086";
const PAYMENT_BASE_URL = __ENV.PAYMENT_BASE_URL || "http://localhost:8087";
const API_VERSION = __ENV.API_VERSION || "v1";
const VUS = Number(__ENV.VUS || "80");
const ITERATIONS = Number(__ENV.ITERATIONS || "2500");
const INCLUDE_PAYMENT_INTENT = (__ENV.INCLUDE_PAYMENT_INTENT || "true").toLowerCase() === "true";
const P95_TARGET_MS = Number(__ENV.CHECKOUT_P95_MS || "320");
const FAIL_RATE_MAX = __ENV.CHECKOUT_FAIL_RATE_MAX || "0.05";
const SUCCESS_MIN = __ENV.CHECKOUT_SUCCESS_MIN || "0.95";

const checkoutSuccessRate = new Rate("checkout_success_rate");

export const options = {
  discardResponseBodies: true,
  scenarios: {
    checkout_flow: {
      executor: "shared-iterations",
      vus: VUS,
      iterations: ITERATIONS,
      maxDuration: __ENV.MAX_DURATION || "120s",
    },
  },
  thresholds: {
    http_req_failed: [`rate<${FAIL_RATE_MAX}`],
    http_req_duration: [`p(95)<${P95_TARGET_MS}`],
    checkout_success_rate: [`rate>${SUCCESS_MIN}`],
  },
};

function headers() {
  return {
    "Content-Type": "application/json",
    "X-API-Version": API_VERSION,
  };
}

function orderPayload(uid, suffix) {
  return {
    userId: uid,
    currency: "INR",
    items: [
      {
        productId: `checkout-product-${suffix}`,
        sku: `SKU-${suffix}`,
        quantity: 1,
        unitPrice: 199.0,
      },
    ],
  };
}

export default function () {
  const uid = 200000 + __VU;
  const suffix = `${__VU}-${__ITER}`;
  const createOrder = http.post(
    `${ORDER_BASE_URL}/api/orders`,
    JSON.stringify(orderPayload(uid, suffix)),
    { headers: headers() },
  );
  const orderCreated = check(createOrder, { "order created": (r) => r.status === 201 });
  if (!orderCreated) {
    checkoutSuccessRate.add(false);
    sleep(0.001);
    return;
  }

  const order = createOrder.json();
  const orderId = order?.id;
  const orderRead = http.get(`${ORDER_BASE_URL}/api/orders/${orderId}`, { headers: headers() });
  const readOk = check(orderRead, { "order read": (r) => r.status === 200 });

  let paymentOk = true;
  if (INCLUDE_PAYMENT_INTENT) {
    const paymentIntent = http.post(
      `${PAYMENT_BASE_URL}/api/payments/intents`,
      JSON.stringify({
        orderId,
        userId: uid,
        amount: 199.0,
        currency: "INR",
        idempotencyKey: `pay-${suffix}`,
      }),
      { headers: headers() },
    );
    paymentOk = check(paymentIntent, {
      "payment intent accepted": (r) => r.status === 201 || r.status === 400,
    });
  }

  checkoutSuccessRate.add(orderCreated && readOk && paymentOk);
  sleep(0.001);
}
