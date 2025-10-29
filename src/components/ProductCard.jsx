import axios from "axios";
import { useAuth } from "../context/AuthContext";
import { ShoppingCart } from "lucide-react";
import { useState, useEffect } from "react";
import Toast from "./Toast";
import { useToast } from "../hooks/useToast";

export default function ProductCard({ p, onProductClick }) {
  const { userId, accessToken } = useAuth();
  const { toast, showToast, hideToast } = useToast();

  const merchant = p.merchants?.[0];
  const actualStock = merchant?.stock || 0;

  async function handleAddToCart() {
    // Check if merchant data exists
    if (!p.merchants?.[0]) {
      showToast("Product does not have merchant data. Please contact support.", "error");
      return;
    }

    if (actualStock <= 0) {
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
          item => item.productId === p.id && item.merchantId === p.merchants[0].merchant_id
        );

        if (existingItemIndex !== -1) {
          const newQuantity = localCart[existingItemIndex].quantity + 1;
          if (newQuantity > actualStock) {
            showToast(`Cannot add more! Only ${actualStock} items available in stock. You already have ${localCart[existingItemIndex].quantity} in your cart.`, "warning", 4000);
            return;
          }
          localCart[existingItemIndex].quantity = newQuantity;
        } else {
          // Add new item to local cart
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

        // Save updated cart to localStorage
        localStorage.setItem("guestCart", JSON.stringify(localCart));
        console.log("🛒 Added to guest cart:", localCart);
        showToast("Product added to cart!", "success");
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
        item => item.productId === p.id && item.merchantId === p.merchants[0].merchant_id
      );

      if (existingItem) {
        const newQuantity = existingItem.quantity + 1;
        if (newQuantity > actualStock) {
          showToast(`Cannot add more! Only ${actualStock} items available in stock. You already have ${existingItem.quantity} in your cart.`, "warning", 4000);
          return;
        }
      }

      const payload = {
        userId, // ✅ required by backend
        productId: p.id,
        merchantId: p.merchants[0].merchant_id, // ✅ Use merchant_id (with underscore)
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
      showToast("Product added to cart!", "success");
    } catch (err) {
      console.error("❌ Add to cart failed:", err);
      const errorMsg = err.response?.data?.message || err.response?.data?.error || err.message;
      showToast("Failed to add to cart: " + errorMsg, "error");
    }
  }

  const stock = actualStock; // Show actual database stock, not calculated available stock

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
      <div className="bg-white border border-gray-200 rounded-lg overflow-hidden hover:shadow-lg transition-shadow duration-300 group cursor-pointer" onClick={() => onProductClick && onProductClick(p)}>
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
            console.error("❌ Product imageUrl from DB:", p.imageUrl);
            console.error("❌ VITE_PRODUCT_SERVICE_URL:", import.meta.env.VITE_PRODUCT_SERVICE_URL);
            e.target.src = "https://via.placeholder.com/400x300?text=Image+Not+Found";
          }}
          onLoad={() => {
            console.log("✅ Image loaded successfully for:", p.name);
            console.log("✅ Image URL:", `${import.meta.env.VITE_PRODUCT_SERVICE_URL}${p.imageUrl}`);
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
        {/* Brand */}
        <div className="text-xs text-gray-500 uppercase tracking-wide mb-1">
          {p.brand}
        </div>

        {/* Product Name */}
        <h3 className="font-medium text-gray-900 mb-4 line-clamp-2 h-12">
          {p.name}
        </h3>

        {/* Price */}
        {merchant && (
          <div className="mb-3">
            <div className="flex items-baseline gap-2">
              <span className="text-2xl font-bold text-gray-900">
                ₹{merchant.price.toFixed(2)}
              </span>
            </div>
            <div className="text-xs text-green-700 font-medium mt-1">
              FREE Delivery
            </div>
          </div>
        )}

        {/* Stock Status */}
        {stock > 0 && stock <= 10 && (
          <div className="text-sm text-orange-600 mb-2">
            Only {stock} left!
          </div>
        )}
        {stock > 10 && (
          <div className="text-sm text-green-600 mb-2">In Stock</div>
        )}

        {/* Add to Cart Button */}
        {merchant && stock > 0 && (
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

