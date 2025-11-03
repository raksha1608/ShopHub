

import { createContext, useContext, useState,useEffect  } from "react";
import { userAPI } from "../api/http";
import axios from "axios";

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [accessToken, setAccessToken] = useState(localStorage.getItem("accessToken"));
  const [role, setRole] = useState(localStorage.getItem("role"));
  const [email, setEmail] = useState(localStorage.getItem("email"));
  const [userId, setUserId] = useState(localStorage.getItem("userId"));
  const [loading, setLoading] = useState(false);

 useEffect(() => {
        const token = localStorage.getItem("accessToken");
        const role = localStorage.getItem("role");
        const email = localStorage.getItem("email");
        const uid = localStorage.getItem("userId");

        if (token) setAccessToken(token);
        if (role) setRole(role);
        if (email) setEmail(email);
        if (uid) setUserId(uid);
 }, []);

  // Helper function to merge guest cart with user cart after login
  async function mergeGuestCart(userId, token) {
    try {
      const guestCart = JSON.parse(localStorage.getItem("guestCart") || "[]");

      if (guestCart.length === 0) {
        console.log("📦 No guest cart items to merge");
        return;
      }

      console.log(`📦 Merging ${guestCart.length} guest cart items with user cart...`);

      // Step 1: Get user's existing cart from backend
      let existingCart = [];
      try {
        const cartRes = await axios.get(
          `${import.meta.env.VITE_CHECKOUT_SERVICE_URL}/cart/get/${userId}`,
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
        );
        existingCart = cartRes.data || [];
        console.log(`📦 User has ${existingCart.length} items in backend cart`);
      } catch (err) {
        console.error("⚠️ Failed to fetch existing cart, assuming empty:", err);
      }

      // Step 2: Merge each guest cart item
      for (const item of guestCart) {
        try {
          // Check if item already exists in user's cart
          const existingItem = existingCart.find(
            (cartItem) =>
              cartItem.productId === item.productId &&
              cartItem.merchantId === item.merchantId
          );

          if (existingItem) {
            // Item exists - use UPDATE to set the quantity (not add)
            // We'll use the guest cart quantity as the final quantity
            console.log(`🔄 Item ${item.productId} exists in user cart (qty: ${existingItem.quantity}), updating to guest cart qty: ${item.quantity}`);

            await axios.put(
              `${import.meta.env.VITE_CHECKOUT_SERVICE_URL}/cart/update`,
              {
                userId,
                productId: item.productId,
                merchantId: item.merchantId,
                price: item.price,
                quantity: item.quantity, // Set to guest cart quantity
              },
              {
                headers: {
                  Authorization: `Bearer ${token}`,
                },
              }
            );
            console.log(`✅ Updated item ${item.productId} to quantity ${item.quantity}`);
          } else {
            // Item doesn't exist - use ADD
            console.log(`➕ Item ${item.productId} doesn't exist in user cart, adding with qty: ${item.quantity}`);

            await axios.post(
              `${import.meta.env.VITE_CHECKOUT_SERVICE_URL}/cart/add`,
              {
                userId,
                productId: item.productId,
                merchantId: item.merchantId,
                price: item.price,
                quantity: item.quantity,
              },
              {
                headers: {
                  Authorization: `Bearer ${token}`,
                },
              }
            );
            console.log(`✅ Added item ${item.productId} with quantity ${item.quantity}`);
          }
        } catch (err) {
          console.error(`❌ Failed to merge item ${item.productId}:`, err);
        }
      }

      // Clear guest cart after successful merge
      localStorage.removeItem("guestCart");
      console.log("✅ Guest cart merged and cleared");

      // Dispatch event to notify components to reload cart
      window.dispatchEvent(new Event('cartMerged'));
    } catch (err) {
      console.error("❌ Failed to merge guest cart:", err);
    }
  }

  // 🧠 LOGIN FUNCTION
  async function login(email, password) {
    setLoading(true);
    try {
      // Step 1: Get token
      const { data } = await userAPI.post("/auth/login", { email, password });
      const token = data.accessToken;
      localStorage.setItem("accessToken", token);
      setAccessToken(token);

      // Step 2: Validate token to get full user info
      const val = await userAPI.get("/auth/validate", {
        headers: { Authorization: `Bearer ${token}` },
      });
      console.log("✅ VALIDATION RESPONSE:", val.data);

      // ✅ Extract and store user info
      const { role, email: em, userId: uid } = val.data;

      localStorage.setItem("role", role);
      localStorage.setItem("email", em);
      localStorage.setItem("userId", uid);

      setRole(role);
      setEmail(em);
      setUserId(uid);

      // Step 3: Merge guest cart with user cart
      await mergeGuestCart(uid, token);

      return role;
    } catch (err) {
      console.error("❌ Login failed:", err);
      throw err;
    } finally {
      setLoading(false);
    }
  }

  // 🧠 LOGOUT FUNCTION
  function logout() {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("role");
    localStorage.removeItem("email");
    localStorage.removeItem("userId");
    setAccessToken(null);
    setRole(null);
    setEmail(null);
    setUserId(null);
  }

  return (
    <AuthContext.Provider value={{ accessToken, role, email, userId, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
