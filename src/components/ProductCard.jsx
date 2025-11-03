import axios from "axios";
import { useAuth } from "../context/AuthContext";
import { ShoppingCart, Plus, Minus } from "lucide-react";
import { useState, useEffect } from "react";
import Toast from "./Toast";
import { useToast } from "../hooks/useToast";

export default function ProductCard({ p, onProductClick }) {
  const { userId, accessToken } = useAuth();
  const { toast, showToast, hideToast } = useToast();
  const [cartQuantity, setCartQuantity] = useState(0); // Track quantity in cart

  const merchant = p.merchants?.[0];
  const actualStock = merchant?.stock || 0;

  // Load cart quantity on mount and when userId changes
  useEffect(() => {
    loadCartQuantity();
  }, [userId, p.id]);

  // Listen for cart merge event after login
  useEffect(() => {
    const handleCartMerged = () => {
      console.log("🔄 Cart merged event received, reloading cart quantity for product:", p.name);
      loadCartQuantity();
    };

    const handleCartUpdated = () => {
      console.log("🔄 Cart updated event received, reloading cart quantity for product:", p.name);
      loadCartQuantity();
    };

    window.addEventListener("cartMerged", handleCartMerged);
    window.addEventListener("cartUpdated", handleCartUpdated);

    return () => {
      window.removeEventListener("cartMerged", handleCartMerged);
      window.removeEventListener("cartUpdated", handleCartUpdated);
    };
  }, [userId, p.id]);

  async function loadCartQuantity() {
    try {
      // If user is not logged in, check localStorage cart
      if (!userId || !accessToken) {
        const localCart = JSON.parse(localStorage.getItem("guestCart") || "[]");
        const existingItem = localCart.find(
          (item) =>
            item.productId === p.id &&
            item.merchantId === p.merchants?.[0]?.merchant_id
        );
        setCartQuantity(existingItem ? existingItem.quantity : 0);
        return;
      }

      // User is logged in - check backend cart
      const cartRes = await axios.get(
        `${import.meta.env.VITE_CHECKOUT_SERVICE_URL}/cart/get/${userId}`,
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );

      const cartItems = cartRes.data.cartItems || cartRes.data || [];
      const existingItem = cartItems.find(
        (item) =>
          item.productId === p.id &&
          item.merchantId === p.merchants?.[0]?.merchant_id
      );
      setCartQuantity(existingItem ? existingItem.quantity : 0);
    } catch (err) {
      console.error("❌ Failed to load cart quantity:", err);
      setCartQuantity(0);
    }
  }

  async function handleAddToCart() {
    // Check if merchant data exists
    if (!p.merchants?.[0]) {
      showToast(
        "Product does not have merchant data. Please contact support.",
        "error"
      );
      return;
    }

    if (actualStock <= 0) {
      showToast("Not enough stock available!", "error");
      return;
    }

    // If user is not logged in, use localStorage cart
    if (!userId || !accessToken) {
      try {
        const localCart = JSON.parse(localStorage.getItem("guestCart") || "[]");

        const existingItemIndex = localCart.findIndex(
          (item) =>
            item.productId === p.id &&
            item.merchantId === p.merchants[0].merchant_id
        );

        if (existingItemIndex !== -1) {
          const newQuantity = localCart[existingItemIndex].quantity + 1;
          if (newQuantity > actualStock) {
            showToast(
              `Only ${actualStock} items available in stock.`,
              "warning",
              4000
            );
            return;
          }
          localCart[existingItemIndex].quantity = newQuantity;
        } else {
          localCart.push({
            productId: p.id,
            merchantId: p.merchants[0].merchant_id,
            price: p.merchants[0].price,
            quantity: 1,
            name: p.name,
            imageUrl: p.imageUrl,
            brand: p.brand,
          });
        }

        localStorage.setItem("guestCart", JSON.stringify(localCart));
        console.log("🛒 Added to guest cart:", localCart);

        const updatedItem = localCart.find(
          (item) =>
            item.productId === p.id &&
            item.merchantId === p.merchants[0].merchant_id
        );
        setCartQuantity(updatedItem ? updatedItem.quantity : 0);
      } catch (err) {
        console.error("❌ Add to guest cart failed:", err);
        showToast("Failed to add to cart", "error");
      }
      return;
    }

    // User is logged in - use backend cart
    try {
      const cartRes = await axios.get(
        `${import.meta.env.VITE_CHECKOUT_SERVICE_URL}/cart/get/${userId}`,
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );

      const cartItems = cartRes.data.cartItems || cartRes.data || [];
      const existingItem = cartItems.find(
        (item) =>
          item.productId === p.id &&
          item.merchantId === p.merchants[0].merchant_id
      );

      if (existingItem) {
        const newQuantity = existingItem.quantity + 1;
        if (newQuantity > actualStock) {
          showToast(
            `Only ${actualStock} items available in stock.`,
            "warning",
            4000
          );
          return;
        }
      }

      const payload = {
        userId,
        productId: p.id,
        merchantId: p.merchants[0].merchant_id,
        price: p.merchants[0].price,
        quantity: 1,
      };

      console.log("🛒 Adding to cart with payload:", payload);

      const res = await axios.post(
        `${import.meta.env.VITE_CHECKOUT_SERVICE_URL}/cart/add`,
        payload,
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );

      console.log("✅ Added to cart:", res.data);
      await loadCartQuantity();
      window.dispatchEvent(new Event("cartUpdated"));
    } catch (err) {
      console.error("❌ Add to cart failed:", err);
      const errorMsg =
        err.response?.data?.message ||
        err.response?.data?.error ||
        err.message;
      showToast("Failed to add to cart: " + errorMsg, "error");
    }
  }

  async function handleIncreaseQuantity() {
    const newQty = cartQuantity + 1;

    if (newQty > actualStock) {
      showToast(
        `Only ${actualStock} items available in stock.`,
        "warning",
        4000
      );
      return;
    }

    if (!userId || !accessToken) {
      try {
        const localCart = JSON.parse(localStorage.getItem("guestCart") || "[]");
        const itemIndex = localCart.findIndex(
          (item) =>
            item.productId === p.id &&
            item.merchantId === p.merchants[0].merchant_id
        );

        if (itemIndex !== -1) {
          localCart[itemIndex].quantity = newQty;
          localStorage.setItem("guestCart", JSON.stringify(localCart));
          setCartQuantity(newQty);
        }
      } catch (err) {
        console.error("❌ Failed to update guest cart:", err);
        showToast("Failed to update quantity", "error");
      }
      return;
    }

    try {
      console.log("📤 Increasing quantity to:", newQty);
      await axios.put(
        `${import.meta.env.VITE_CHECKOUT_SERVICE_URL}/cart/update`,
        {
          userId: parseInt(userId),
          productId: p.id,
          merchantId: p.merchants[0].merchant_id,
          price: p.merchants[0].price,
          quantity: newQty,
        },
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );
      setCartQuantity(newQty);
      window.dispatchEvent(new Event("cartUpdated"));
      console.log("✅ Backend cart increased to quantity:", newQty);
    } catch (err) {
      console.error("❌ Failed to increase quantity:", err);
      console.error("Error details:", err.response?.data);
      showToast("Failed to update quantity", "error");
    }
  }

  async function handleDecreaseQuantity() {
    console.log("🔽 Decrease quantity clicked, current quantity:", cartQuantity);
    const newQty = cartQuantity - 1;

    if (newQty <= 0) {
      console.log("🗑️ Quantity will be 0, removing from cart completely");
      try {
        await handleRemoveFromCart();
        console.log("✅ Item removed, cartQuantity should now be 0");
      } catch (err) {
        console.error("❌ Error during removal:", err);
      }
      return;
    }

    console.log("📉 Decreasing quantity to:", newQty);

    if (!userId || !accessToken) {
      try {
        const localCart = JSON.parse(localStorage.getItem("guestCart") || "[]");
        const itemIndex = localCart.findIndex(
          (item) =>
            item.productId === p.id &&
            item.merchantId === p.merchants[0].merchant_id
        );

        if (itemIndex !== -1) {
          localCart[itemIndex].quantity = newQty;
          localStorage.setItem("guestCart", JSON.stringify(localCart));
          setCartQuantity(newQty);
          console.log("✅ Guest cart updated to quantity:", newQty);
        }
      } catch (err) {
        console.error("❌ Failed to update guest cart:", err);
        showToast("Failed to update quantity", "error");
      }
      return;
    }

    try {
      console.log("📤 Sending update request to backend:", {
        userId: parseInt(userId),
        productId: p.id,
        merchantId: p.merchants[0].merchant_id,
        quantity: newQty,
      });

      await axios.put(
        `${import.meta.env.VITE_CHECKOUT_SERVICE_URL}/cart/update`,
        {
          userId: parseInt(userId),
          productId: p.id,
          merchantId: p.merchants[0].merchant_id,
          price: p.merchants[0].price,
          quantity: newQty,
        },
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );
      setCartQuantity(newQty);
      window.dispatchEvent(new Event("cartUpdated"));
      console.log("✅ Backend cart decreased to quantity:", newQty);
    } catch (err) {
      console.error("❌ Failed to decrease quantity:", err);
      console.error("Error details:", err.response?.data);
      showToast("Failed to update quantity", "error");
    }
  }

  async function handleRemoveFromCart() {
    if (!userId || !accessToken) {
      try {
        const localCart = JSON.parse(localStorage.getItem("guestCart") || "[]");
        const updatedCart = localCart.filter(
          (item) =>
            !(
              item.productId === p.id &&
              item.merchantId === p.merchants[0].merchant_id
            )
        );
        localStorage.setItem("guestCart", JSON.stringify(updatedCart));
        setCartQuantity(0);
        window.dispatchEvent(new Event("cartUpdated"));
        console.log("✅ Removed from guest cart");
      } catch (err) {
        console.error("❌ Failed to remove from guest cart:", err);
        showToast("Failed to remove from cart", "error");
      }
      return;
    }

    try {
      console.log("🗑️ Removing from cart:", {
        userId: parseInt(userId),
        productId: p.id,
        merchantId: p.merchants[0].merchant_id,
      });

      await axios.delete(
        `${import.meta.env.VITE_CHECKOUT_SERVICE_URL}/cart/remove`,
        {
          data: {
            userId: parseInt(userId),
            productId: p.id,
            merchantId: p.merchants[0].merchant_id,
            price: p.merchants[0].price,
            quantity: 1,
          },
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );

      console.log("✅ Successfully removed from cart");
      setCartQuantity(0);
      window.dispatchEvent(new Event("cartUpdated"));
    } catch (err) {
      console.error("❌ Failed to remove from cart:", err);
      console.error("Error details:", err.response?.data);
      showToast(
        "Failed to remove from cart: " +
          (err.response?.data?.error || err.message),
        "error"
      );
    }
  }

  const stock = actualStock;

  return (
    <>
      {/* ✅ Toast positioned at top-right corner */}
      {toast && (
        <div className="fixed top-0 right-0 z-50">
          <Toast
            message={toast.message}
            type={toast.type}
            onClose={hideToast}
            duration={toast.duration}
          />
        </div>
      )}

      <div
        className="bg-white border border-gray-200 rounded-lg overflow-hidden hover:shadow-lg transition-shadow duration-300 group cursor-pointer"
        onClick={() => onProductClick && onProductClick(p)}
      >
        {/* Product Image */}
        <div className="relative bg-gray-50 h-64 flex items-center justify-center overflow-hidden">
          <img
            src={
              p.imageUrl
                ? `${import.meta.env.VITE_PRODUCT_SERVICE_URL}${p.imageUrl}`
                : "https://via.placeholder.com/400x300?text=No+Image"
            }
            alt={p.name}
            className="w-full h-full object-contain group-hover:scale-105 transition-transform duration-300"
            onError={(e) => {
              console.error("❌ Image failed to load for product:", p.name);
              console.error("❌ Image URL:", e.target.src);
              e.target.src =
                "https://via.placeholder.com/400x300?text=Image+Not+Found";
            }}
          />
          {stock <= 10 && stock > 0 && (
            <div className="absolute top-2 left-2 bg-red-500 text-white text-xs px-2 py-1 rounded">
              Only {stock} left!
            </div>
          )}
          {stock === 0 && (
            <div className="absolute top-2 left-2 bg-gray-500 text-white text-xs px-2 py-1 rounded">
              Out of Stock
            </div>
          )}
        </div>

        {/* Product Details */}
        <div className="p-4">
          <div className="text-xs text-gray-500 uppercase tracking-wide mb-1">
            {p.brand}
          </div>
          <h3 className="font-medium text-gray-900 mb-4 line-clamp-2 h-12">
            {p.name}
          </h3>

          {merchant && (
            <div className="mb-3">
              <div className="flex items-baseline gap-2">
                <span className="text-2xl font-bold text-gray-900">
                  ₹{merchant.price.toFixed(2)}
                </span>
              </div>
            </div>
          )}

          {stock > 0 && stock <= 10 && (
            <div className="text-sm text-orange-600 mb-2">
              Only {stock} left!
            </div>
          )}
          {stock > 10 && (
            <div className="text-sm text-green-600 mb-2">In Stock</div>
          )}

          {merchant && stock > 0 && (
            <>
              {cartQuantity === 0 ? (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    handleAddToCart();
                  }}
                  className="w-full bg-[#FFD814] hover:bg-[#F7CA00] text-gray-900 font-medium py-2 px-4 rounded-lg transition-colors duration-200 flex items-center justify-center gap-2"
                >
                  <ShoppingCart size={18} />
                  Add to Cart
                </button>
              ) : (
                <div className="flex items-center gap-2">
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDecreaseQuantity();
                    }}
                    className="flex-1 bg-gray-200 hover:bg-gray-300 text-gray-900 font-bold py-2 px-3 rounded-lg transition-colors duration-200 flex items-center justify-center"
                  >
                    <Minus size={18} />
                  </button>
                  <div className="flex-1 text-center font-bold text-lg text-gray-900">
                    {cartQuantity}
                  </div>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleIncreaseQuantity();
                    }}
                    className="flex-1 bg-[#FFD814] hover:bg-[#F7CA00] text-gray-900 font-bold py-2 px-3 rounded-lg transition-colors duration-200 flex items-center justify-center"
                  >
                    <Plus size={18} />
                  </button>
                </div>
              )}
            </>
          )}

          {stock === 0 && (
            <button
              disabled
              className="w-full bg-gray-300 text-gray-500 font-medium py-2 px-4 rounded-lg cursor-not-allowed"
            >
              Out of Stock
            </button>
          )}
        </div>
      </div>
    </>
  );
}
