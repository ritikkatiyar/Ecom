import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8083";
const API_VERSION = __ENV.API_VERSION || "v1";
const VUS = Number(__ENV.VUS || "120");
const ITERATIONS = Number(__ENV.ITERATIONS || "6000");
const P95_TARGET_MS = Number(__ENV.BROWSE_P95_MS || "180");
const FAIL_RATE_MAX = __ENV.BROWSE_FAIL_RATE_MAX || "0.02";

let seededProductIds = [];

export const options = {
  discardResponseBodies: true,
  scenarios: {
    browse_products: {
      executor: "shared-iterations",
      vus: VUS,
      iterations: ITERATIONS,
      maxDuration: __ENV.MAX_DURATION || "90s",
    },
  },
  thresholds: {
    http_req_failed: [`rate<${FAIL_RATE_MAX}`],
    http_req_duration: [`p(95)<${P95_TARGET_MS}`],
  },
};

function headers() {
  return {
    "Content-Type": "application/json",
    "X-API-Version": API_VERSION,
  };
}

export function setup() {
  const ids = [];
  for (let i = 0; i < 10; i += 1) {
    const payload = JSON.stringify({
      name: `Load Product ${Date.now()}-${i}`,
      description: "k6 seed product",
      category: "load-test",
      brand: "k6",
      price: 99.99 + i,
      colors: ["black"],
      sizes: ["M"],
      active: true,
    });
    const response = http.post(`${BASE_URL}/api/products`, payload, { headers: headers() });
    check(response, { "seed product created": (r) => r.status === 201 });
    if (response.status === 201) {
      const body = response.json();
      if (body && body.id) {
        ids.push(body.id);
      }
    }
  }
  seededProductIds = ids;
  return { ids };
}

export default function (data) {
  const categoryResponse = http.get(
    `${BASE_URL}/api/products?page=0&size=20&category=load-test&sortBy=name&direction=asc`,
    { headers: headers() },
  );
  check(categoryResponse, { "browse list 200": (r) => r.status === 200 });

  const searchResponse = http.get(
    `${BASE_URL}/api/products?page=0&size=20&q=Load&sortBy=name&direction=asc`,
    { headers: headers() },
  );
  check(searchResponse, { "search list 200": (r) => r.status === 200 });

  if (data.ids.length > 0) {
    const id = data.ids[(__ITER + __VU) % data.ids.length];
    const detailResponse = http.get(`${BASE_URL}/api/products/${id}`, { headers: headers() });
    check(detailResponse, { "product detail 200": (r) => r.status === 200 });
  }

  sleep(0.001);
}
