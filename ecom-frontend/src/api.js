export async function apiRequest(path, options = {}) {
  const token = localStorage.getItem("accessToken");
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {})
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(path, {
    ...options,
    headers
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `HTTP ${response.status}`);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

export const authApi = {
  signup: (payload) => apiRequest("/api/auth/signup", { method: "POST", body: JSON.stringify(payload) }),
  login: (payload) => apiRequest("/api/auth/login", { method: "POST", body: JSON.stringify(payload) }),
  refresh: (refreshToken) =>
    apiRequest("/api/auth/refresh", { method: "POST", body: JSON.stringify({ refreshToken }) }),
  logout: (refreshToken) =>
    apiRequest("/api/auth/logout", {
      method: "POST",
      body: JSON.stringify({ refreshToken })
    }),
  validate: () => apiRequest("/api/auth/validate")
};

export const productApi = {
  list: (params = {}) => {
    const qp = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && `${v}`.trim() !== "") {
        qp.set(k, v);
      }
    });
    const suffix = qp.toString() ? `?${qp.toString()}` : "";
    return apiRequest(`/api/products${suffix}`);
  },
  create: (payload) => apiRequest("/api/products", { method: "POST", body: JSON.stringify(payload) })
};

export const cartApi = {
  get: ({ userId, guestId }) => {
    const qp = new URLSearchParams();
    if (userId) qp.set("userId", userId);
    if (guestId) qp.set("guestId", guestId);
    return apiRequest(`/api/cart?${qp.toString()}`);
  },
  addItem: (payload) => apiRequest("/api/cart/items", { method: "POST", body: JSON.stringify(payload) }),
  removeItem: ({ productId, userId, guestId }) => {
    const qp = new URLSearchParams();
    if (userId) qp.set("userId", userId);
    if (guestId) qp.set("guestId", guestId);
    return apiRequest(`/api/cart/items/${encodeURIComponent(productId)}?${qp.toString()}`, { method: "DELETE" });
  },
  clear: ({ userId, guestId }) => {
    const qp = new URLSearchParams();
    if (userId) qp.set("userId", userId);
    if (guestId) qp.set("guestId", guestId);
    return apiRequest(`/api/cart?${qp.toString()}`, { method: "DELETE" });
  },
  merge: ({ userId, guestId }) => apiRequest("/api/cart/merge", { method: "POST", body: JSON.stringify({ userId, guestId }) })
};
