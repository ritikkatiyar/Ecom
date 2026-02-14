import { useEffect, useState } from "react";
import { authApi, cartApi, productApi } from "./api";

const initialProduct = {
  name: "",
  description: "",
  category: "",
  brand: "",
  price: "",
  colors: "",
  sizes: "",
  active: true
};

const initialCartItem = {
  productId: "",
  quantity: 1
};

function getOrCreateGuestId() {
  const existing = localStorage.getItem("guestId");
  if (existing) {
    return existing;
  }
  const generated = (globalThis.crypto && globalThis.crypto.randomUUID)
    ? globalThis.crypto.randomUUID()
    : `guest-${Date.now()}`;
  localStorage.setItem("guestId", generated);
  return generated;
}

export default function App() {
  const [email, setEmail] = useState("test@example.com");
  const [password, setPassword] = useState("Password123");
  const [role, setRole] = useState("CUSTOMER");
  const [query, setQuery] = useState({ q: "", category: "", brand: "", page: 0, size: 10 });
  const [products, setProducts] = useState([]);
  const [total, setTotal] = useState(0);
  const [productForm, setProductForm] = useState(initialProduct);
  const [status, setStatus] = useState("Ready");
  const [tokenState, setTokenState] = useState({ accessToken: "", refreshToken: "" });

  const [cartOwnerMode, setCartOwnerMode] = useState("GUEST");
  const [cartUserId, setCartUserId] = useState("1");
  const [guestId, setGuestId] = useState("");
  const [cartItemForm, setCartItemForm] = useState(initialCartItem);
  const [cart, setCart] = useState({ ownerType: "", ownerId: "", totalItems: 0, items: [] });

  useEffect(() => {
    const accessToken = localStorage.getItem("accessToken") || "";
    const refreshToken = localStorage.getItem("refreshToken") || "";
    setTokenState({ accessToken, refreshToken });
    setGuestId(getOrCreateGuestId());
  }, []);

  async function run(label, fn) {
    try {
      setStatus(`${label}...`);
      await fn();
      setStatus(`${label} done`);
    } catch (err) {
      setStatus(`${label} failed: ${err.message}`);
    }
  }

  function saveTokens(tokenResponse) {
    localStorage.setItem("accessToken", tokenResponse.accessToken);
    localStorage.setItem("refreshToken", tokenResponse.refreshToken);
    setTokenState({ accessToken: tokenResponse.accessToken, refreshToken: tokenResponse.refreshToken });
  }

  function cartOwnerParams() {
    if (cartOwnerMode === "USER") {
      return { userId: Number(cartUserId), guestId: null };
    }
    return { userId: null, guestId };
  }

  async function handleSignup() {
    await run("Signup", async () => {
      const data = await authApi.signup({ email, password, role });
      saveTokens(data);
    });
  }

  async function handleLogin() {
    await run("Login", async () => {
      const data = await authApi.login({ email, password });
      saveTokens(data);
    });
  }

  async function handleRefresh() {
    await run("Refresh", async () => {
      const data = await authApi.refresh(tokenState.refreshToken);
      saveTokens(data);
    });
  }

  async function handleValidate() {
    await run("Validate", async () => {
      const data = await authApi.validate();
      setStatus(`Token active: ${data.active}`);
    });
  }

  async function handleLogout() {
    await run("Logout", async () => {
      await authApi.logout(tokenState.refreshToken);
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      setTokenState({ accessToken: "", refreshToken: "" });
    });
  }

  async function loadProducts() {
    await run("Load products", async () => {
      const page = await productApi.list(query);
      setProducts(page.content || []);
      setTotal(page.totalElements || 0);
    });
  }

  async function createProduct() {
    await run("Create product", async () => {
      await productApi.create({
        ...productForm,
        price: Number(productForm.price),
        colors: productForm.colors.split(",").map((x) => x.trim()).filter(Boolean),
        sizes: productForm.sizes.split(",").map((x) => x.trim()).filter(Boolean)
      });
      setProductForm(initialProduct);
      await loadProducts();
    });
  }

  async function loadCart() {
    await run("Load cart", async () => {
      const data = await cartApi.get(cartOwnerParams());
      setCart(data);
    });
  }

  async function addToCart() {
    await run("Add to cart", async () => {
      const payload = {
        ...cartOwnerParams(),
        productId: cartItemForm.productId,
        quantity: Number(cartItemForm.quantity)
      };
      const data = await cartApi.addItem(payload);
      setCart(data);
    });
  }

  async function removeCartItem(productId) {
    await run("Remove cart item", async () => {
      const data = await cartApi.removeItem({ ...cartOwnerParams(), productId });
      setCart(data);
    });
  }

  async function clearCart() {
    await run("Clear cart", async () => {
      await cartApi.clear(cartOwnerParams());
      setCart({ ownerType: cartOwnerMode, ownerId: cartOwnerMode === "USER" ? cartUserId : guestId, totalItems: 0, items: [] });
    });
  }

  async function mergeGuestToUser() {
    await run("Merge guest cart", async () => {
      const data = await cartApi.merge({ userId: Number(cartUserId), guestId });
      setCartOwnerMode("USER");
      setCart(data);
    });
  }

  return (
    <div className="container">
      <h1>Ecom Dev Console</h1>
      <p className="status">{status}</p>

      <section>
        <h2>Auth</h2>
        <div className="grid">
          <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="email" />
          <input value={password} onChange={(e) => setPassword(e.target.value)} placeholder="password" type="password" />
          <input value={role} onChange={(e) => setRole(e.target.value)} placeholder="role" />
        </div>
        <div className="row">
          <button onClick={handleSignup}>Signup</button>
          <button onClick={handleLogin}>Login</button>
          <button onClick={handleRefresh} disabled={!tokenState.refreshToken}>Refresh</button>
          <button onClick={handleValidate} disabled={!tokenState.accessToken}>Validate</button>
          <button onClick={handleLogout} disabled={!tokenState.accessToken}>Logout</button>
          <a href="/oauth2/authorization/google" target="_blank" rel="noreferrer">Google OAuth</a>
        </div>
      </section>

      <section>
        <h2>Products</h2>
        <div className="grid">
          <input value={query.q} onChange={(e) => setQuery({ ...query, q: e.target.value })} placeholder="q" />
          <input value={query.category} onChange={(e) => setQuery({ ...query, category: e.target.value })} placeholder="category" />
          <input value={query.brand} onChange={(e) => setQuery({ ...query, brand: e.target.value })} placeholder="brand" />
          <button onClick={loadProducts}>Fetch</button>
        </div>
        <p>Total: {total}</p>
        <ul>
          {products.map((p) => (
            <li key={p.id}>{p.name} | {p.brand} | {p.category} | ${p.price}</li>
          ))}
        </ul>
      </section>

      <section>
        <h2>Create Product</h2>
        <div className="grid">
          <input value={productForm.name} onChange={(e) => setProductForm({ ...productForm, name: e.target.value })} placeholder="name" />
          <input value={productForm.description} onChange={(e) => setProductForm({ ...productForm, description: e.target.value })} placeholder="description" />
          <input value={productForm.category} onChange={(e) => setProductForm({ ...productForm, category: e.target.value })} placeholder="category" />
          <input value={productForm.brand} onChange={(e) => setProductForm({ ...productForm, brand: e.target.value })} placeholder="brand" />
          <input value={productForm.price} onChange={(e) => setProductForm({ ...productForm, price: e.target.value })} placeholder="price" type="number" step="0.01" />
          <input value={productForm.colors} onChange={(e) => setProductForm({ ...productForm, colors: e.target.value })} placeholder="colors comma separated" />
          <input value={productForm.sizes} onChange={(e) => setProductForm({ ...productForm, sizes: e.target.value })} placeholder="sizes comma separated" />
          <button onClick={createProduct}>Create</button>
        </div>
      </section>

      <section>
        <h2>Cart</h2>
        <div className="grid">
          <select value={cartOwnerMode} onChange={(e) => setCartOwnerMode(e.target.value)}>
            <option value="GUEST">Guest Cart</option>
            <option value="USER">User Cart</option>
          </select>
          <input value={guestId} onChange={(e) => setGuestId(e.target.value)} placeholder="guestId" disabled={cartOwnerMode !== "GUEST"} />
          <input value={cartUserId} onChange={(e) => setCartUserId(e.target.value)} placeholder="userId" disabled={cartOwnerMode !== "USER"} />
          <button onClick={loadCart}>Load Cart</button>
        </div>

        <div className="grid">
          <input value={cartItemForm.productId} onChange={(e) => setCartItemForm({ ...cartItemForm, productId: e.target.value })} placeholder="productId" />
          <input value={cartItemForm.quantity} onChange={(e) => setCartItemForm({ ...cartItemForm, quantity: e.target.value })} placeholder="quantity" type="number" min="1" />
          <button onClick={addToCart}>Add Item</button>
          <button onClick={clearCart}>Clear Cart</button>
        </div>

        <div className="row">
          <button onClick={mergeGuestToUser}>Merge Guest -> User</button>
        </div>

        <p>
          Cart Owner: {cart.ownerType} {cart.ownerId} | Total Items: {cart.totalItems}
        </p>
        <ul>
          {(cart.items || []).map((item) => (
            <li key={item.productId}>
              {item.productId} x {item.quantity}
              <button onClick={() => removeCartItem(item.productId)}>Remove</button>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
