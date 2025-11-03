import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { Lock, Mail, Eye, EyeOff } from "lucide-react";
import axios from "axios";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();
  const { login } = useAuth();

  const mergeGuestCart = async (userId, accessToken) => {
    try {
      const guestCart = JSON.parse(localStorage.getItem("guestCart") || "[]");

      if (guestCart.length === 0) {
        console.log("📦 No guest cart items to merge");
        return;
      }

      console.log(`📦 Merging ${guestCart.length} guest cart items to user account...`);

      // Add each guest cart item to the user's cart
      for (const item of guestCart) {
        try {
          const payload = {
            userId: Number(userId),
            productId: item.productId,
            merchantId: item.merchantId,
            price: item.price,
            quantity: item.quantity,
          };

          await axios.post(
            `${import.meta.env.VITE_CHECKOUT_SERVICE_URL}/cart/add`,
            payload,
            {
              headers: {
                Authorization: `Bearer ${accessToken}`,
              },
            }
          );
          console.log(`✅ Merged item: ${item.name}`);
        } catch (err) {
          console.error(`❌ Failed to merge item ${item.name}:`, err);
          // Continue with other items even if one fails
        }
      }

      // Clear guest cart after successful merge
      localStorage.removeItem("guestCart");
      console.log("✅ Guest cart merged and cleared");

      // Dispatch custom event to notify ProductCard components to reload cart
      window.dispatchEvent(new Event('cartMerged'));
    } catch (err) {
      console.error("❌ Failed to merge guest cart:", err);
    }
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      const role = await login(email, password);

      // Merge guest cart for END_USER role
      if (role === "END_USER") {
        const userId = localStorage.getItem("userId");
        const accessToken = localStorage.getItem("accessToken");
        await mergeGuestCart(userId, accessToken);
      }

      if (role === "MERCHANT") {
        navigate("/merchant/dashboard", { replace: true });
      } else {
        navigate("/products", { replace: true });
      }
    } catch (err) {
      console.error("❌ Login failed:", err);
      setError("Invalid email or password. Please try again.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100 flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <Link to="/" className="inline-flex items-center gap-1">
            <span className="text-4xl font-bold text-gray-900">ShopHub</span>
            <span className="text-orange-400 text-5xl leading-none">.</span>
          </Link>
        </div>

        {/* Login Card */}
        <div className="bg-white rounded-lg shadow-xl p-8 border border-gray-200">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Sign In</h1>
          <p className="text-gray-600 mb-6">Welcome back! Please enter your details.</p>

          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
              <div className="text-red-600 text-sm">{error}</div>
            </div>
          )}

          <form onSubmit={handleLogin} className="space-y-5" autoComplete="off">
            {/* Email Field */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Email Address
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Mail size={20} className="text-gray-400" />
                </div>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  placeholder="Enter your email"
                  autoComplete="off"
                  className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                />
              </div>
            </div>

            {/* Password Field */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Password
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Lock size={20} className="text-gray-400" />
                </div>
                <input
                  type={showPassword ? "text" : "password"}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  placeholder="Enter your password"
                  autoComplete="new-password"
                  className="w-full pl-10 pr-12 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center"
                >
                  {showPassword ? (
                    <EyeOff size={20} className="text-gray-400 hover:text-gray-600" />
                  ) : (
                    <Eye size={20} className="text-gray-400 hover:text-gray-600" />
                  )}
                </button>
              </div>
            </div>

            {/* Sign In Button */}
            <button
              type="submit"
              disabled={busy}
              className="w-full bg-[#FFD814] hover:bg-[#F7CA00] text-gray-900 font-bold py-3 px-4 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {busy ? "Signing in..." : "Sign In"}
            </button>
          </form>

          {/* Divider */}
          <div className="relative my-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-300"></div>
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="px-2 bg-white text-gray-500">New to ShopHub?</span>
            </div>
          </div>

          {/* Create Account Button */}
          <Link
            to="/register"
            className="block w-full text-center bg-white border-2 border-gray-300 hover:bg-gray-50 text-gray-900 font-medium py-3 px-4 rounded-lg transition-colors"
          >
            Create your ShopHub account
          </Link>
        </div>
      </div>
    </div>
  );
}

