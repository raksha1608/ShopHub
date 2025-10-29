import { useNavigate, useLocation } from "react-router-dom";
import { useState, useEffect } from "react";
import { ShoppingCart, User, Package, LogOut, Search, X } from "lucide-react";
import { checkoutAPI } from "../api/http";

export default function Navbar() {
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [role, setRole] = useState("");
  const [cartCount, setCartCount] = useState(0);
  const [userId, setUserId] = useState(null);

  // Clear search query when navigating away from products page or when URL doesn't have search param
  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const searchParam = params.get("search");

    if (!searchParam) {
      setSearchQuery("");
    } else {
      setSearchQuery(searchParam);
    }
  }, [location]);

  useEffect(() => {
    const token = localStorage.getItem("accessToken");
    const storedUserId = localStorage.getItem("userId");
    if (!token) return;
    try {
      const payload = JSON.parse(atob(token.split(".")[1]));
      setEmail(payload.sub || "");
      setRole(payload.role || "");
      setUserId(storedUserId);
    } catch (err) {
      console.error("Invalid token", err);
    }
  }, []);

  // Fetch cart count
  useEffect(() => {
    async function fetchCartCount() {
      // If user is not logged in, get count from localStorage
      if (!userId) {
        const guestCart = JSON.parse(localStorage.getItem("guestCart") || "[]");
        setCartCount(guestCart.length);
        return;
      }

      // User is logged in - fetch from backend
      try {
        const res = await checkoutAPI.get(`/cart/get/${userId}`);
        const cartItems = res.data.cartItems || res.data || [];
        setCartCount(cartItems.length);
      } catch (err) {
        console.error("Failed to fetch cart count:", err);
        setCartCount(0);
      }
    }

    fetchCartCount();

    // Poll cart count every 3 seconds to keep it updated
    const interval = setInterval(fetchCartCount, 3000);
    return () => clearInterval(interval);
  }, [userId]);

  const handleLogout = () => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("userId");
    localStorage.removeItem("role");
    localStorage.removeItem("email");
    // Don't clear guestCart - user might want to keep items after logout
    navigate("/login");
  };

  const handleSearch = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/products?search=${encodeURIComponent(searchQuery)}`);
    } else {
      // Clear search and show all products
      setSearchQuery("");
      navigate("/products");
    }
  };

  const handleLogoClick = () => {
    // Clear search when clicking logo
    setSearchQuery("");
    navigate("/");
  };

  const isLoggedIn = !!email;

  return (
    <nav className="bg-[#131921] text-white sticky top-0 z-50 shadow-lg">
      {/* Main Navigation Bar */}
      <div className="px-6 py-3">
        <div className="max-w-[1600px] mx-auto flex items-center justify-between gap-6">
          {/* Logo */}
          <div
            className="flex items-center gap-1 hover:border border-white px-3 py-1 cursor-pointer transition-all flex-shrink-0"
            onClick={handleLogoClick}
          >
            <span className="text-2xl font-bold tracking-tight">ShopHub</span>
            <span className="text-orange-400 text-3xl leading-none">.</span>
          </div>

          {/* Search Bar */}
          <form onSubmit={handleSearch} className="flex-1 max-w-3xl mx-6">
            <div className="flex relative">
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search for products, brands and more..."
                className="flex-1 px-4 py-2.5 pr-12 rounded-l-md text-gray-900 focus:outline-none focus:ring-2 focus:ring-orange-400"
              />
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => {
                    setSearchQuery("");
                    navigate("/products");
                  }}
                  className="absolute right-14 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700 transition-colors"
                >
                  <X size={20} />
                </button>
              )}
              <button
                type="submit"
                className="bg-[#FEBD69] hover:bg-[#F3A847] px-6 rounded-r-md transition-colors flex items-center justify-center"
              >
                <Search size={20} className="text-gray-900" />
              </button>
            </div>
          </form>

          {/* Right Side Links */}
          <div className="flex items-center gap-6 flex-shrink-0">
            {/* User Account */}
            {isLoggedIn ? (
              <div
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  navigate("/profile");
                }}
                className="hover:border border-white px-3 py-1 cursor-pointer transition-all"
              >
                <div className="text-xs">Hello, {email.split("@")[0]}</div>
                <div className="text-sm font-bold flex items-center gap-1">
                  <User size={16} />
                  Account
                </div>
              </div>
            ) : (
              <div
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  navigate("/login");
                }}
                className="hover:border border-white px-3 py-1 cursor-pointer transition-all"
              >
                <div className="text-xs">Hello, Guest</div>
                <div className="text-sm font-bold">Sign In</div>
              </div>
            )}

            {/* Orders */}
            {isLoggedIn && (
              <div
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  navigate("/orders");
                }}
                className="hover:border border-white px-3 py-1 cursor-pointer transition-all"
              >
                <div className="text-xs">Returns</div>
                <div className="text-sm font-bold flex items-center gap-1">
                  <Package size={16} />
                  Orders
                </div>
              </div>
            )}

            {/* Merchant Dashboard */}
            {role === "MERCHANT" && (
              <div
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  navigate("/merchant/dashboard");
                }}
                className="hover:border border-white px-3 py-1 cursor-pointer transition-all"
              >
                <div className="text-xs">Merchant</div>
                <div className="text-sm font-bold flex items-center gap-1">
                  <Package size={16} />
                  Dashboard
                </div>
              </div>
            )}

            {/* Cart */}
            <div
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                navigate("/cart");
              }}
              className="hover:border border-white px-3 py-1 cursor-pointer transition-all flex items-center gap-2"
            >
              <div className="relative">
                <ShoppingCart size={28} />
                {cartCount > 0 && (
                  <span className="absolute -top-1 -right-1 bg-orange-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center font-bold">
                    {cartCount}
                  </span>
                )}
              </div>
              <span className="text-sm font-bold mt-3">Cart</span>
            </div>

            {/* Logout */}
            {isLoggedIn && (
              <button
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  handleLogout();
                }}
                className="hover:border border-white px-3 py-2 transition-all flex items-center gap-2"
              >
                <LogOut size={20} />
                <span className="text-sm font-bold">Logout</span>
              </button>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}
