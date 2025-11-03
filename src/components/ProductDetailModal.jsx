import { X, ShoppingCart, Package, Shield, Plus, Minus } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { useState, useEffect } from "react";
import axios from "axios";
import Toast from "./Toast";
import { useToast } from "../hooks/useToast";

export default function ProductDetailModal({ product, onClose }) {
  const { userId, accessToken } = useAuth();
  const { toast, showToast, hideToast } = useToast();
  const [cartQuantity, setCartQuantity] = useState(0); // Track quantity in cart

  if (!product) return null;

  const merchant = product.merchants?.[0];
  const stock = merchant?.stock || 0;

  // Load cart quantity on mount and when userId changes
  useEffect(() => {
    loadCartQuantity();
  }, [userId, product.id]);

  // Listen for cart merge event after login
  useEffect(() => {
    const handleCartMerged = () => {
      console.log("🔄 [Modal] Cart merged event received, reloading cart quantity for product:", product.name);
      loadCartQuantity();
    };

    window.addEventListener('cartMerged', handleCartMerged);

    return () => {
      window.removeEventListener('cartMerged', handleCartMerged);
    };
  }, [userId, product.id]);

  async function loadCartQuantity() {
    try {
      // If user is not logged in, check localStorage cart
      if (!userId || !accessToken) {
        const localCart = JSON.parse(localStorage.getItem("guestCart") || "[]");
        const existingItem = localCart.find(
          item => item.productId === product.id && item.merchantId === merchant?.merchant_id
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
        item => item.productId === product.id && item.merchantId === merchant?.merchant_id
      );
      setCartQuantity(existingItem ? existingItem.quantity : 0);
    } catch (err) {
      console.error("❌ [Modal] Failed to load cart quantity:", err);
      setCartQuantity(0);
    }
  }

  async function handleAddToCart() {
    if (!merchant) {
      showToast("Product does not have merchant data. Please contact support.", "error");
      return;
    }

    if (stock <= 0) {
      showToast("Not enough stock available!", "error");
      return;
    }

    // If user is not logged in, use localStorage cart
    if (!userId || !accessToken) {
      try {
        // Get existing cart from localStorage
        const localCart = JSON.parse(localStorage.getItem("guestCart") || "[]");

        // Check if item already exists in local cart
        const existingItemIndex = localCart.findIndex(
          item => item.productId === product.id && item.merchantId === merchant.merchant_id
        );

        if (existingItemIndex !== -1) {
          const newQuantity = localCart[existingItemIndex].quantity + 1;
          if (newQuantity > stock) {
            showToast(`Only ${stock} items available in stock.`, "warning", 4000);
            return;
          }
          localCart[existingItemIndex].quantity = newQuantity;
        } else {
          // Add new item to local cart
          localCart.push({
            productId: product.id,
            merchantId: merchant.merchant_id,
            price: merchant.price,
            quantity: 1,
            name: product.name,
            imageUrl: product.imageUrl,
            brand: product.brand,
          });
        }

        // Save updated cart to localStorage
        localStorage.setItem("guestCart", JSON.stringify(localCart));
        console.log("🛒 Added to guest cart:", localCart);
        await loadCartQuantity(); // Reload to update UI
        // No toast - just update UI
      } catch (err) {
        console.error("❌ Add to guest cart failed:", err);
        showToast("Failed to add to cart", "error");
      }
      return;
    }

    // User is logged in - use backend cart
    try {
      // First, check if item already exists in cart
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
        item => item.productId === product.id && item.merchantId === merchant.merchant_id
      );

      if (existingItem) {
        const newQuantity = existingItem.quantity + 1;
        if (newQuantity > stock) {
          showToast(`Only ${stock} items available in stock.`, "warning", 4000);
          return;
        }
      }

      const payload = {
        userId,
        productId: product.id,
        merchantId: merchant.merchant_id,
        price: merchant.price,
        quantity: 1,
      };

      console.log("🛒 [Modal] Adding to cart with payload:", payload);

      const res = await axios.post(
        `${import.meta.env.VITE_CHECKOUT_SERVICE_URL}/cart/add`,
        payload,
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );

      console.log("✅ [Modal] Added to cart:", res.data);
      await loadCartQuantity(); // Reload to update UI
      // Dispatch event to notify ProductCard components
      window.dispatchEvent(new Event('cartUpdated'));
      // No toast - just update UI
    } catch (err) {
      console.error("❌ [Modal] Add to cart failed:", err);
      showToast("Failed to add to cart: " + (err.response?.data?.error || err.message), "error");
    }
  }

  async function handleIncreaseQuantity() {
    const newQty = cartQuantity + 1;

    if (newQty > stock) {
      showToast(`Only ${stock} items available in stock.`, "warning", 4000);
      return;
    }

    // If user is not logged in, update localStorage cart
    if (!userId || !accessToken) {
      try {
        const localCart = JSON.parse(localStorage.getItem("guestCart") || "[]");
        const itemIndex = localCart.findIndex(
          item => item.productId === product.id && item.merchantId === merchant.merchant_id
        );

        if (itemIndex !== -1) {
          localCart[itemIndex].quantity = newQty;
          localStorage.setItem("guestCart", JSON.stringify(localCart));
          setCartQuantity(newQty);
          window.dispatchEvent(new Event('cartUpdated'));
        }
      } catch (err) {
        console.error("❌ [Modal] Failed to update guest cart:", err);
        showToast("Failed to update quantity", "error");
      }
      return;
    }

    // User is logged in - update backend cart
    try {
      console.log("📤 [Modal] Increasing quantity to:", newQty);
      await axios.put(
        `${import.meta.env.VITE_CHECKOUT_SERVICE_URL}/cart/update`,
        {
          userId: parseInt(userId),
          productId: product.id,
          merchantId: merchant.merchant_id,
          price: merchant.price,
          quantity: newQty,
        },
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );
      setCartQuantity(newQty);
      window.dispatchEvent(new Event('cartUpdated'));
      console.log("✅ [Modal] Backend cart increased to quantity:", newQty);
    } catch (err) {
      console.error("❌ [Modal] Failed to increase quantity:", err);
      console.error("Error details:", err.response?.data);
      showToast("Failed to update quantity", "error");
    }
  }

  async function handleDecreaseQuantity() {
    console.log("🔽 [Modal] Decrease quantity clicked, current quantity:", cartQuantity);
    const newQty = cartQuantity - 1;

    if (newQty <= 0) {
      // Remove item from cart completely
      console.log("🗑️ [Modal] Quantity will be 0, removing from cart completely");
      try {
        await handleRemoveFromCart();
        console.log("✅ [Modal] Item removed, cartQuantity should now be 0");
      } catch (err) {
        console.error("❌ [Modal] Error during removal:", err);
      }
      return;
    }

    console.log("📉 [Modal] Decreasing quantity to:", newQty);

    // If user is not logged in, update localStorage cart
    if (!userId || !accessToken) {
      try {
        const localCart = JSON.parse(localStorage.getItem("guestCart") || "[]");
        const itemIndex = localCart.findIndex(
          item => item.productId === product.id && item.merchantId === merchant.merchant_id
        );

        if (itemIndex !== -1) {
          localCart[itemIndex].quantity = newQty;
          localStorage.setItem("guestCart", JSON.stringify(localCart));
          setCartQuantity(newQty);
          window.dispatchEvent(new Event('cartUpdated'));
          console.log("✅ [Modal] Guest cart updated to quantity:", newQty);
        }
      } catch (err) {
        console.error("❌ [Modal] Failed to update guest cart:", err);
        showToast("Failed to update quantity", "error");
      }
      return;
    }

    // User is logged in - update backend cart by setting new quantity
    try {
      console.log("[Modal] Sending update request to backend:", {
        userId: parseInt(userId),
        productId: product.id,
        merchantId: merchant.merchant_id,
        quantity: newQty
      });

      await axios.put(
        `${import.meta.env.VITE_CHECKOUT_SERVICE_URL}/cart/update`,
        {
          userId: parseInt(userId),
          productId: product.id,
          merchantId: merchant.merchant_id,
          price: merchant.price,
          quantity: newQty,
        },
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );
      setCartQuantity(newQty);
      window.dispatchEvent(new Event('cartUpdated'));
      console.log("✅ [Modal] Backend cart decreased to quantity:", newQty);
    } catch (err) {
      console.error("❌ [Modal] Failed to decrease quantity:", err);
      console.error("Error details:", err.response?.data);
      showToast("Failed to update quantity", "error");
    }
  }

  async function handleRemoveFromCart() {
    // If user is not logged in, remove from localStorage cart
    if (!userId || !accessToken) {
      try {
        const localCart = JSON.parse(localStorage.getItem("guestCart") || "[]");
        const updatedCart = localCart.filter(
          item => !(item.productId === product.id && item.merchantId === merchant.merchant_id)
        );
        localStorage.setItem("guestCart", JSON.stringify(updatedCart));
        setCartQuantity(0);
        window.dispatchEvent(new Event('cartUpdated'));
        console.log("✅ [Modal] Removed from guest cart");
      } catch (err) {
        console.error("❌ [Modal] Failed to remove from guest cart:", err);
        showToast("Failed to remove from cart", "error");
      }
      return;
    }

    // User is logged in - remove from backend cart
    try {
      console.log("🗑️ [Modal] Removing from cart:", {
        userId: parseInt(userId),
        productId: product.id,
        merchantId: merchant.merchant_id
      });

      await axios.delete(
        `${import.meta.env.VITE_CHECKOUT_SERVICE_URL}/cart/remove`,
        {
          data: {
            userId: parseInt(userId),
            productId: product.id,
            merchantId: merchant.merchant_id,
            price: merchant.price, // Required by backend validation
            quantity: 1, // Required by backend validation (min value is 1)
          },
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );

      console.log("✅ [Modal] Successfully removed from cart");
      setCartQuantity(0);
      window.dispatchEvent(new Event('cartUpdated'));
    } catch (err) {
      console.error("❌ [Modal] Failed to remove from cart:", err);
      console.error("Error details:", err.response?.data);
      showToast("Failed to remove from cart: " + (err.response?.data?.error || err.message), "error");
    }
  }

  return (
    <>
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={hideToast}
          duration={toast.duration}
        />
      )}
      <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4" onClick={onClose}>
        <div className="bg-white rounded-lg max-w-5xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        {/* Close Button */}
        <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex justify-between items-center">
          <h2 className="text-2xl font-bold text-gray-900">Product Details</h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-full transition-colors"
          >
            <X size={24} className="text-gray-600" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6">
          <div className="grid md:grid-cols-2 gap-8">
            {/* Left: Image */}
            <div className="bg-gray-50 rounded-lg p-8 flex items-center justify-center">
              <img
                src={
                  product.imageUrl
                    ? `${import.meta.env.VITE_PRODUCT_SERVICE_URL}${product.imageUrl}`
                    : "https://via.placeholder.com/600x600?text=No+Image"
                }
                alt={product.name}
                className="max-w-full max-h-[500px] object-contain"
                onError={(e) => {
                  console.error("❌ Image failed to load:", e.target.src);
                  e.target.src = "https://via.placeholder.com/600x600?text=Image+Not+Found";
                }}
              />
            </div>

            {/* Right: Details */}
            <div className="flex flex-col">
              {/* Brand */}
              <div className="text-sm text-blue-600 font-medium mb-2">
                {product.brand}
              </div>

              {/* Product Name */}
              <h1 className="text-3xl font-bold text-gray-900 mb-6">
                {product.name}
              </h1>

              {/* Price */}
              {merchant && (
                <div className="mb-6 pb-6 border-b border-gray-200">
                  <div className="flex items-baseline gap-2 mb-2">
                    <span className="text-2xl font-bold text-gray-900">
                      ₹{merchant.price.toFixed(2)}
                    </span>
                  </div>

                </div>
              )}

              {/* Stock Status */}
              <div className="mb-6">
                {stock > 0 && stock <= 10 && (
                  <div className="flex items-center gap-2 text-orange-600 mb-2">
                    <Package size={20} />
                    <span className="font-medium">Only {stock} left!</span>
                  </div>
                )}
                {stock > 10 && (
                  <div className="flex items-center gap-2 text-green-600 mb-2">
                    <Package size={20} />
                    <span className="font-medium">In Stock</span>
                  </div>
                )}
                {stock === 0 && (
                  <div className="flex items-center gap-2 text-red-600 mb-2">
                    <Package size={20} />
                    <span className="font-medium">Out of Stock</span>
                  </div>
                )}
              </div>

              {/* Description */}
              <div className="mb-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-2">About this item</h3>
                <p className="text-gray-700 leading-relaxed">{product.description}</p>
              </div>

              {/* Attributes */}
              {product.attributes && Object.keys(product.attributes).length > 0 && (
                <div className="mb-6">
                  <h3 className="text-lg font-semibold text-gray-900 mb-3">Specifications</h3>
                  <div className="bg-gray-50 rounded-lg p-4 space-y-2">
                    {Object.entries(product.attributes).map(([key, value]) => (
                      value && (
                        <div key={key} className="flex justify-between">
                          <span className="text-gray-600 capitalize">{key}:</span>
                          <span className="text-gray-900 font-medium">{value}</span>
                        </div>
                      )
                    ))}
                  </div>
                </div>
              )}

              {/* Features */}
{/*               <div className="mb-6 space-y-3"> */}
{/*                 <div className="flex items-center gap-3 text-gray-700"> */}
{/*                   <Shield size={20} className="text-green-600" /> */}
{/*                   <span>1 year warranty included</span> */}
{/*                 </div> */}
{/*               </div> */}

              {/* Add to Cart Button or Quantity Controls */}
              {merchant && stock > 0 && (
                <>
                  {cartQuantity === 0 ? (
                    <button
                      onClick={handleAddToCart}
                      className="w-full bg-[#FFD814] hover:bg-[#F7CA00] text-gray-900 font-bold py-4 px-6 rounded-lg transition-colors duration-200 flex items-center justify-center gap-3 text-lg"
                    >
                      <ShoppingCart size={24} />
                      Add to Cart
                    </button>
                  ) : (
                    <div className="flex items-center gap-4">
                      <button
                        onClick={handleDecreaseQuantity}
                        className="bg-gray-200 hover:bg-gray-300 text-gray-800 font-bold py-3 px-5 rounded-lg transition-colors duration-200 flex items-center justify-center"
                      >
                        <Minus size={20} />
                      </button>
                      <span className="text-2xl font-bold text-gray-900 min-w-[60px] text-center">
                        {cartQuantity}
                      </span>
                      <button
                        onClick={handleIncreaseQuantity}
                        className="bg-gray-200 hover:bg-gray-300 text-gray-800 font-bold py-3 px-5 rounded-lg transition-colors duration-200 flex items-center justify-center"
                      >
                        <Plus size={20} />
                      </button>
                    </div>
                  )}
                </>
              )}
              {stock === 0 && (
                <button
                  disabled
                  className="w-full bg-gray-300 text-gray-500 font-bold py-4 px-6 rounded-lg cursor-not-allowed text-lg"
                >
                  Out of Stock
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
      </div>
    </>
  );
}

