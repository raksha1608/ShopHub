import { useLocation, useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar";
import { checkoutAPI } from "../api/http";
import { useAuth } from "../context/AuthContext";
import { useState } from "react";
import Toast from "../components/Toast";
import { useToast } from "../hooks/useToast";

export default function CheckoutPage() {
  const { state } = useLocation();
  const navigate = useNavigate();
  const { userId } = useAuth();
  const [placing, setPlacing] = useState(false);
  const { toast, showToast, hideToast } = useToast();

  async function placeOrder() {
    if (!userId) {
      // Redirect directly to login without showing error message
      navigate("/login");
      return;
    }

    if (!state || !state.cart || state.cart.length === 0) {
      showToast("Cart is empty!", "error");
      setTimeout(() => navigate("/cart"), 2000);
      return;
    }

    setPlacing(true);
    try {
      console.log("📦 Starting order placement...");
      console.log("📦 User ID:", userId);
      console.log("📦 Total Amount:", state.total);
      console.log("📦 Cart Items:", state.cart);

      // Prepare order data
      const orderData = {
        userId: Number(userId),
        totalAmount: Number(state.total),
        items: state.cart.map(c => ({
          productId: String(c.productId),
          merchantId: String(c.merchantId),
          quantity: Number(c.quantity),
          price: Number(c.price)
        }))
      };

      console.log("📦 Sending order data:", JSON.stringify(orderData, null, 2));

      // Make the API call
      const response = await checkoutAPI.post("/orders/checkout", orderData);

      console.log("✅ Full response:", response);
      console.log("✅ Response status:", response.status);
      console.log("✅ Response data:", response.data);

      // Check if order was successful
      if (response.data && (response.data.success || response.data.order_id)) {
        const orderId = response.data.order_id || response.data.orderId || "N/A";
        console.log("✅ Order placed successfully! Order ID:", orderId);

        // Clear cart items after successful order
        try {
          console.log("🧹 Clearing cart items...");
          for (const item of state.cart) {
            await checkoutAPI.delete("/cart/remove", { data: item });
          }
          console.log("✅ Cart cleared successfully");
        } catch (clearErr) {
          console.warn("⚠️ Failed to clear cart:", clearErr);
          // Don't fail the order if cart clearing fails
        }

        // Show success message
        showToast(`Order Placed Successfully! Order ID: ${orderId}`, "success", 3000);

        // Navigate to orders page after showing toast
        setTimeout(() => {
          navigate("/orders");
        }, 2000);
      } else {
        throw new Error("Order placement failed - no success confirmation received");
      }
    } catch (err) {
      console.error("❌ Checkout failed:", err);
      console.error("❌ Error name:", err.name);
      console.error("❌ Error message:", err.message);
      console.error("❌ Error response:", err.response);
      console.error("❌ Error response data:", err.response?.data);
      console.error("❌ Error response status:", err.response?.status);

      let errorMsg = "Network error. Please check your connection and try again.";

      if (err.response) {
        // Server responded with error
        errorMsg = err.response?.data?.message
          || err.response?.data?.error
          || `Server error (${err.response.status}). Please try again.`;
      } else if (err.request) {
        // Request made but no response
        errorMsg = "No response from server. Please check if services are running.";
      } else {
        // Something else happened
        errorMsg = err.message || "Can't place order. Please try again.";
      }

      showToast(errorMsg, "error", 4000);
    } finally {
      setPlacing(false);
    }
  }

  if (!state || !state.cart) {
    return (
      <div className="min-h-screen bg-gray-100">
        <Navbar />
        <div className="max-w-2xl mx-auto px-4 py-20 text-center">
          <div className="bg-white rounded-lg shadow-sm p-12">
            <div className="text-6xl mb-4">🛒</div>
            <h2 className="text-3xl font-bold text-gray-800 mb-2">Cart is empty!</h2>
            <p className="text-gray-600 mb-6">Add some products to checkout</p>
            <button
              onClick={() => navigate("/products")}
              className="bg-[#FFD814] hover:bg-[#F7CA00] text-gray-900 font-medium px-8 py-3 rounded-lg transition-colors"
            >
              Start Shopping
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100">
      <Navbar />
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={hideToast}
          duration={toast.duration}
        />
      )}
      <div className="max-w-3xl mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">Confirm Your Order</h1>
        <div className="bg-white rounded-lg p-8 shadow-sm border border-gray-200">
          <p className="text-lg text-gray-700 mb-6 font-medium">Review your order details:</p>
          
          {/* Order Items */}
          <div className="space-y-3 mb-6">
            {state.cart.map((item, i) => {
              const productInfo = state.productDetails?.[item.productId];
              const productName = productInfo?.name || `Product ${item.productId}`;
              const productImage = productInfo?.imageUrl;

              return (
                <div key={i} className="flex justify-between items-center bg-gray-50 p-4 rounded-lg">
                  <div className="flex items-center gap-4">
                    {/* Product Image */}
                    {productImage && (
                      <img
                        src={`${import.meta.env.VITE_PRODUCT_SERVICE_URL}${productImage}`}
                        alt={productName}
                        className="w-16 h-16 object-contain rounded"
                        onError={(e) => {
                          e.target.style.display = 'none';
                        }}
                      />
                    )}
                    <div>
                      <div className="font-medium text-gray-900">{productName}</div>
                      <div className="text-sm text-gray-600">Quantity: {item.quantity} × ₹{item.price.toFixed(2)}</div>
                    </div>
                  </div>
                  <div className="text-lg font-bold text-gray-900">
                    ₹{(item.price * item.quantity).toFixed(2)}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Total */}
          <div className="border-t pt-4 mb-6">
            <div className="flex justify-between items-center text-2xl font-bold">
              <span>Total Amount:</span>
              <span className="text-[#131921]">₹{state.total.toFixed(2)}</span>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="space-y-3">
            <button
              onClick={placeOrder}
              disabled={placing}
              className="w-full bg-[#FFD814] hover:bg-[#F7CA00] text-gray-900 font-bold py-3 rounded-lg transition-colors disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {placing ? (
                <>
                  <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-gray-900"></div>
                  Placing Order...
                </>
              ) : (
                "Place Order"
              )}
            </button>
            <button
              onClick={() => navigate("/cart")}
              disabled={placing}
              className="w-full bg-white border-2 border-gray-300 hover:bg-gray-50 text-gray-900 font-medium py-3 rounded-lg transition-colors disabled:opacity-60"
            >
              Back to Cart
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

