import http from "k6/http";
import { check, sleep } from "k6";
import { Rate } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8085";
const API_VERSION = __ENV.API_VERSION || "v1";
const VUS = Number(__ENV.VUS || "100");
const ITERATIONS = Number(__ENV.ITERATIONS || "5000");
const P95_TARGET_MS = Number(__ENV.CART_P95_MS || "220");
const FAIL_RATE_MAX = __ENV.CART_FAIL_RATE_MAX || "0.03";
const CONSISTENCY_MIN = __ENV.CART_CONSISTENCY_MIN || "0.99";

const cartConsistencyRate = new Rate("cart_consistency_rate");

export const options = {
  discardResponseBodies: true,
  scenarios: {
    cart_operations: {
      executor: "shared-iterations",
      vus: VUS,
      iterations: ITERATIONS,
      maxDuration: __ENV.MAX_DURATION || "90s",
    },
  },
  thresholds: {
    http_req_failed: [`rate<${FAIL_RATE_MAX}`],
    http_req_duration: [`p(95)<${P95_TARGET_MS}`],
    cart_consistency_rate: [`rate>${CONSISTENCY_MIN}`],
  },
};

function headers() {
  return {
    "Content-Type": "application/json",
    "X-API-Version": API_VERSION,
  };
}

function userId() {
  return 100000 + __VU;
}

export default function () {
  const uid = userId();
  const productId = `load-product-${__VU}-${__ITER % 5}`;
  const quantity = (__ITER % 3) + 1;

  const addResponse = http.post(
    `${BASE_URL}/api/cart/items`,
    JSON.stringify({
      userId: uid,
      productId,
      quantity,
    }),
    { headers: headers() },
  );
  check(addResponse, { "cart add created": (r) => r.status === 201 });

  const getResponse = http.get(`${BASE_URL}/api/cart?userId=${uid}`, { headers: headers() });
  const getOk = check(getResponse, { "cart get 200": (r) => r.status === 200 });

  let consistent = false;
  if (getOk) {
    const cart = getResponse.json();
    const totalItems = Number(cart?.totalItems || 0);
    const items = Array.isArray(cart?.items) ? cart.items : [];
    const itemCount = items.reduce((sum, item) => sum + Number(item.quantity || 0), 0);
    consistent = totalItems === itemCount;
  }
  cartConsistencyRate.add(consistent);

  if (__ITER % 10 === 0) {
    const removeResponse = http.del(
      `${BASE_URL}/api/cart/items/${productId}?userId=${uid}`,
      null,
      { headers: headers() },
    );
    check(removeResponse, { "cart remove 200": (r) => r.status === 200 });
  }

  sleep(0.001);
}
