import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import Navbar from "../components/Navbar";
import ProductCard from "../components/ProductCard";
import ProductDetailModal from "../components/ProductDetailModal";
import { ChevronRight, TrendingUp, ChevronLeft } from "lucide-react";

export default function ProductPage() {
  const [products, setProducts] = useState([]);
  const [searchParams] = useSearchParams();
  const searchTerm = searchParams.get("search") || "";
  const [category, setCategory] = useState("");
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const PRODUCTS_PER_PAGE = 16;

  // Fetch products
  useEffect(() => {
    if (searchTerm) {
      // Use search service when there's a search term
      searchProducts();
    } else {
      // Use product service for browsing
      fetchProducts();
    }
    // Reset to page 1 when category or search term changes
    setCurrentPage(1);
  }, [category, searchTerm]);

  async function fetchProducts() {
    try {
      const url = `${import.meta.env.VITE_PRODUCT_SERVICE_URL}/products${
        category ? `?category=${category}` : ""
      }`;
      const res = await fetch(url);
      const data = await res.json();
      // Sort products by newest first (reverse order - assuming newer products have higher IDs)
      const sortedData = [...data].reverse();
      setProducts(sortedData);
    } catch (err) {
      console.error("❌ Error fetching products:", err);
    }
  }

  async function searchProducts() {
    try {
      // Use product service search endpoint directly
      const searchUrl = `${import.meta.env.VITE_PRODUCT_SERVICE_URL}/products/search?q=${encodeURIComponent(searchTerm)}`;
      console.log("🔍 Searching with URL:", searchUrl);
      const res = await fetch(searchUrl);

      if (!res.ok) {
        throw new Error(`Search returned ${res.status}`);
      }

      const data = await res.json();
      console.log("✅ Search results:", data);

      // Sort search results by newest first (reverse order)
      const sortedData = [...data].reverse();
      setProducts(sortedData);
    } catch (err) {
      console.error("❌ Error searching products:", err);
      // Fallback to showing all products if search fails
      fetchProducts();
    }
  }

  // Filter products based on category when using search
  const filteredProducts = searchTerm
    ? category
      ? products.filter((p) => p.category === category)
      : products
    : products.filter((p) =>
        category ? p.category === category : true
      );

  // Pagination calculations
  const totalPages = Math.ceil(filteredProducts.length / PRODUCTS_PER_PAGE);
  const startIndex = (currentPage - 1) * PRODUCTS_PER_PAGE;
  const endIndex = startIndex + PRODUCTS_PER_PAGE;
  const currentProducts = filteredProducts.slice(startIndex, endIndex);

  // Generate page numbers to display
  const getPageNumbers = () => {
    const pages = [];
    const maxPagesToShow = 5;

    if (totalPages <= maxPagesToShow) {
      // Show all pages if total is less than max
      for (let i = 1; i <= totalPages; i++) {
        pages.push(i);
      }
    } else {
      // Always show first page
      pages.push(1);

      // Calculate range around current page
      let startPage = Math.max(2, currentPage - 1);
      let endPage = Math.min(totalPages - 1, currentPage + 1);

      // Add ellipsis after first page if needed
      if (startPage > 2) {
        pages.push('...');
      }

      // Add pages around current page
      for (let i = startPage; i <= endPage; i++) {
        pages.push(i);
      }

      // Add ellipsis before last page if needed
      if (endPage < totalPages - 1) {
        pages.push('...');
      }

      // Always show last page
      pages.push(totalPages);
    }

    return pages;
  };

  const handlePageChange = (page) => {
    if (page >= 1 && page <= totalPages) {
      setCurrentPage(page);
      // Scroll to top of products section
      window.scrollTo({ top: 400, behavior: 'smooth' });
    }
  };

  const categories = [
    { name: "All", value: "", icon: "🏪" },
    { name: "Electronics", value: "Electronics", icon: "📱" },
    { name: "Fashion", value: "Fashion", icon: "👗" },
    { name: "Home & Kitchen", value: "Home & Kitchen", icon: "🏠" },
    { name: "Beauty & Personal Care", value: "Beauty & Personal Care", icon: "💄" },
    { name: "Sports & Outdoors", value: "Sports & Outdoors", icon: "⚽" },
    { name: "Books & Stationery", value: "Books & Stationery", icon: "📚" },
    { name: "Toys & Baby Products", value: "Toys & Baby Products", icon: "🧸" },
  ];

  return (
    <div className="min-h-screen bg-gray-100">
      <Navbar />

      {/* Hero Banner */}
      <div className="bg-gradient-to-r from-blue-600 via-purple-600 to-pink-600 text-white">
        <div className="max-w-[1500px] mx-auto px-4 py-12">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-4xl font-bold mb-2">Welcome to ShopHub</h1>
              <p className="text-xl mb-4">Your one-stop shop for everything you need</p>
            </div>
            <div className="hidden lg:block">
              <img
                src="https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?w=400&h=300&fit=crop"
                alt="Shopping"
                className="rounded-lg shadow-2xl w-80 h-60 object-cover"
              />
            </div>
          </div>
        </div>
      </div>

      {/* Category Tabs */}
      <div className="bg-white border-b sticky top-[120px] z-40 shadow-sm">
        <div className="max-w-[1500px] mx-auto px-4 py-4">
          <div className="flex gap-2 overflow-x-auto">
            {categories.map((cat) => (
              <button
                key={cat.value}
                onClick={() => setCategory(cat.value)}
                className={`flex items-center gap-2 px-6 py-2 rounded-full font-medium transition-all whitespace-nowrap ${
                  category === cat.value
                    ? "bg-[#131921] text-white"
                    : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                }`}
              >
                <span>{cat.icon}</span>
                <span>{cat.name}</span>
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-[1500px] mx-auto px-4 py-8">
        {/* Products Count */}
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-2xl font-bold text-gray-800">
            {category
              ? `${categories.find((c) => c.value === category)?.name} Products`
              : "All Products"}
          </h2>
          <span className="text-gray-600">
            {filteredProducts.length} {filteredProducts.length === 1 ? "product" : "products"} found
            {totalPages > 1 && (
              <span className="ml-2 text-sm">
                (Page {currentPage} of {totalPages})
              </span>
            )}
          </span>
        </div>

        {/* Product Grid */}
        {filteredProducts.length > 0 ? (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {currentProducts.map((product) => (
                <ProductCard
                  key={product.id}
                  p={product}
                  onProductClick={setSelectedProduct}
                />
              ))}
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="mt-12 mb-8 flex items-center justify-center gap-2">
                {/* Previous Button */}
                <button
                  onClick={() => handlePageChange(currentPage - 1)}
                  disabled={currentPage === 1}
                  className={`flex items-center gap-1 px-4 py-2 rounded-lg font-medium transition-all ${
                    currentPage === 1
                      ? "bg-gray-200 text-gray-400 cursor-not-allowed"
                      : "bg-white text-gray-700 hover:bg-[#131921] hover:text-white border border-gray-300"
                  }`}
                >
                  <ChevronLeft size={18} />
                  <span>Previous</span>
                </button>

                {/* Page Numbers */}
                <div className="flex items-center gap-2">
                  {getPageNumbers().map((page, index) => (
                    page === '...' ? (
                      <span key={`ellipsis-${index}`} className="px-2 text-gray-500">
                        ...
                      </span>
                    ) : (
                      <button
                        key={page}
                        onClick={() => handlePageChange(page)}
                        className={`w-10 h-10 rounded-full font-medium transition-all ${
                          currentPage === page
                            ? "bg-[#131921] text-white shadow-lg scale-110"
                            : "bg-white text-gray-700 hover:bg-gray-100 border border-gray-300"
                        }`}
                      >
                        {page}
                      </button>
                    )
                  ))}
                </div>

                {/* Next Button */}
                <button
                  onClick={() => handlePageChange(currentPage + 1)}
                  disabled={currentPage === totalPages}
                  className={`flex items-center gap-1 px-4 py-2 rounded-lg font-medium transition-all ${
                    currentPage === totalPages
                      ? "bg-gray-200 text-gray-400 cursor-not-allowed"
                      : "bg-white text-gray-700 hover:bg-[#131921] hover:text-white border border-gray-300"
                  }`}
                >
                  <span>Next</span>
                  <ChevronRight size={18} />
                </button>
              </div>
            )}
          </>
        ) : (
          <div className="text-center py-20">
            <div className="text-6xl mb-4">🔍</div>
            <h3 className="text-2xl font-bold text-gray-700 mb-2">No products found</h3>
            <p className="text-gray-500">Try adjusting your search or filters</p>
          </div>
        )}
      </div>

      {/* Product Detail Modal */}
      {selectedProduct && (
        <ProductDetailModal
          product={selectedProduct}
          onClose={() => setSelectedProduct(null)}
        />
      )}

      {/* Footer Banner */}
      <div className="bg-[#232F3E] text-white mt-12">
        <div className="max-w-[1500px] mx-auto px-4 py-12">
          <div className="grid grid-cols-1 gap-8">
            <div>
              <h3 className="text-xl font-bold mb-4">About ShopHub</h3>
              <p className="text-gray-300">
                Your trusted online marketplace for quality products at the best prices.
              </p>
            </div>
          </div>
          <div className="border-t border-gray-700 mt-8 pt-8 text-center text-gray-400">
            <p>&copy; 2025 ShopHub. All rights reserved.</p>
          </div>
        </div>
      </div>
    </div>
  );
}

