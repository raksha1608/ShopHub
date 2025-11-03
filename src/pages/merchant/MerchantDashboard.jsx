import { useEffect, useState } from "react";
import { productAPI, merchantAPI } from "../../api/http";
import { useAuth } from "../../context/AuthContext";
import MerchantLayout from "../../layouts/MerchantLayout";
import { Package, Upload, DollarSign, Star, Box, Trash2, Edit } from "lucide-react";
import Toast from "../../components/Toast";
import { useToast } from "../../hooks/useToast";
import ConfirmModal from "../../components/ConfirmModal";

const VALID_CATEGORIES = [
  "Electronics",
  "Fashion",
  "Home & Kitchen",
  "Beauty & Personal Care",
  "Sports & Outdoors",
  "Books & Stationery",
  "Toys & Baby Products"
];

export default function MerchantDashboard() {
  const { email, userId } = useAuth();
  const [form, setForm] = useState({
    name: "",
    brand: "",
    category: "",
    description: "",
    attributes: { color: "", storage: "" },
    merchants: [
      { merchantId: 1, name: "ShopX", price: "", stock: "" },
    ],
  });
  const [image, setImage] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [list, setList] = useState([]);
  const [busy, setBusy] = useState(false);
  const [merchantId, setMerchantId] = useState(null);
  const { toast, showToast, hideToast } = useToast();
  const [confirmDelete, setConfirmDelete] = useState(null); // { productId, productName }
  const [editingProduct, setEditingProduct] = useState(null); // Product being edited
  const [editForm, setEditForm] = useState(null); // Form data for editing
  const [editImage, setEditImage] = useState(null); // New image for editing
  const [editImagePreview, setEditImagePreview] = useState(null); // Preview for edit image

  // Fetch merchant ID from userId
  useEffect(() => {
    async function fetchMerchantId() {
      if (!userId) {
        console.log("⏳ Waiting for userId...");
        return;
      }
      try {
        console.log("🔍 Fetching merchant for userId:", userId);
        const { data } = await merchantAPI.get(`/merchants/user/${userId}`);
        console.log("✅ Merchant data received:", data);
        const mId = data.merchantId || data.id;
        console.log("✅ Setting merchantId to:", mId);
        setMerchantId(mId);
        // Update form with merchant ID
        setForm(prev => ({
          ...prev,
          merchants: [{ ...prev.merchants[0], merchantId: mId }]
        }));
      } catch (err) {
        console.error("❌ Failed to fetch merchant ID:", err);
        console.error("❌ Error details:", err.response?.data);
        // If merchant doesn't exist, use userId as fallback
        console.log("⚠️ Using userId as merchantId fallback:", userId);
        setMerchantId(parseInt(userId));
        setForm(prev => ({
          ...prev,
          merchants: [{ ...prev.merchants[0], merchantId: parseInt(userId) }]
        }));
      }
    }
    fetchMerchantId();
  }, [userId]);

  async function load() {
    try {
      const { data } = await productAPI.get("/products");
      const allProducts = data ?? [];
      console.log(`📦 Total products in database: ${allProducts.length}`);

      // Filter to show only products for the logged-in merchant
      const effectiveMerchantId = merchantId || parseInt(userId);
      console.log("🔍 Filtering for merchantId:", effectiveMerchantId);

      if (effectiveMerchantId !== null && effectiveMerchantId !== undefined) {
        const myProducts = allProducts.filter(product => {
          const hasMerchant = product.merchants?.some(m => {
            console.log(`Checking product ${product.name}: merchant_id=${m.merchant_id}, looking for=${effectiveMerchantId}`);
            return m.merchant_id === effectiveMerchantId || m.merchantId === effectiveMerchantId;
          });
          return hasMerchant;
        });
        console.log(`✅ Showing ${myProducts.length} products for merchant ${effectiveMerchantId}`);
        setList(myProducts);
      } else {
        console.log("⚠️ No merchantId or userId available");
        setList([]);
      }
    } catch (err) {
      console.error("❌ Failed to load products:", err);
      setList([]);
    }
  }

  useEffect(() => {
    if (merchantId || userId) {
      console.log("🔄 Loading products for merchantId:", merchantId, "or userId:", userId);
      load();
    }
  }, [merchantId, userId]);

  const handleImageChange = (e) => {
    const file = e.target.files?.[0];
    if (file) {
      setImage(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagePreview(reader.result);
      };
      reader.readAsDataURL(file);
    }
  };

  async function handleAdd(e) {
    e.preventDefault();
    if (!image) {
      showToast("Please choose a product image", "warning");
      return;
    }
    if (!merchantId && !userId) {
      showToast("User ID not loaded yet. Please wait a moment and try again.", "error");
      return;
    }
    setBusy(true);
    try {
      const token = localStorage.getItem("accessToken");
      const effectiveMerchantId = merchantId || parseInt(userId);
      console.log("📦 Adding product with merchantId:", effectiveMerchantId);

      // Ensure form has the correct merchantId (use merchant_id for backend)
      const productData = {
        ...form,
        merchants: [{
          merchant_id: effectiveMerchantId,  // Backend expects merchant_id (snake_case)
          name: form.merchants[0].name,
          price: parseFloat(form.merchants[0].price),
          stock: parseInt(form.merchants[0].stock)
        }]
      };

      const fd = new FormData();
      fd.append(
        "product",
        new Blob([JSON.stringify(productData)], { type: "application/json" })
      );
      fd.append("image", image);

      await productAPI.post("/products/add", fd, {
        headers: {
          "Content-Type": "multipart/form-data",
          "Authorization": `Bearer ${token}`
        },
      });
      console.log("✅ Product added, reloading list...");
      await load();
      showToast("Product added successfully!", "success");
      setForm({
        name: "",
        brand: "",
        category: "",
        description: "",
        attributes: { color: "", storage: "" },
        merchants: [
          { merchantId: effectiveMerchantId, name: "ShopX", price: "", stock: "" },
        ],
      });
      setImage(null);
      setImagePreview(null);
      // Reset file input
      const fileInput = document.getElementById("product-image");
      if (fileInput) fileInput.value = "";
    } catch (err) {
      console.error("❌ Failed to add product:", err);
      showToast("Failed to add product: " + (err.response?.data?.error || err.message), "error");
    } finally {
      setBusy(false);
    }
  }

  function handleDeleteClick(productId, productName) {
    setConfirmDelete({ productId, productName });
  }

  async function handleDeleteConfirm() {
    if (!confirmDelete) return;

    const { productId } = confirmDelete;
    setConfirmDelete(null); // Close modal

    try {
      console.log("🗑️ Deleting product:", productId);
      await productAPI.delete(`/products/${productId}`);
      console.log("✅ Product deleted, reloading list...");
      await load();
      showToast("Product deleted successfully!", "success");
    } catch (err) {
      console.error("❌ Failed to delete product:", err);
      showToast("Failed to delete product: " + (err.response?.data?.error || err.message), "error");
    }
  }

  function handleDeleteCancel() {
    setConfirmDelete(null);
  }

  function handleEditClick(product) {
    const effectiveMerchantId = merchantId || parseInt(userId);
    setEditingProduct(product);
    setEditForm({
      name: product.name,
      brand: product.brand,
      category: product.category,
      description: product.description,
      attributes: product.attributes || { color: "", storage: "" },
      merchants: [{
        merchantId: effectiveMerchantId,
        name: product.merchants?.[0]?.name || "ShopX",
        price: product.merchants?.[0]?.price || "",
        stock: product.merchants?.[0]?.stock || ""
      }]
    });
    setEditImagePreview(product.imageUrl ? `${import.meta.env.VITE_PRODUCT_SERVICE_URL}${product.imageUrl}` : null);
    setEditImage(null);
  }

  function handleEditCancel() {
    setEditingProduct(null);
    setEditForm(null);
    setEditImage(null);
    setEditImagePreview(null);
  }

  async function handleEditSubmit(e) {
    e.preventDefault();
    if (!editForm.name || !editForm.category) {
      showToast("Please fill in all required fields", "warning");
      return;
    }

    setBusy(true);
    try {
      const token = localStorage.getItem("accessToken");
      const effectiveMerchantId = merchantId || parseInt(userId);

      const productData = {
        ...editForm,
        merchants: [{
          merchant_id: effectiveMerchantId,
          name: editForm.merchants[0].name,
          price: parseFloat(editForm.merchants[0].price),
          stock: parseInt(editForm.merchants[0].stock)
        }]
      };

      const fd = new FormData();
      fd.append(
        "product",
        new Blob([JSON.stringify(productData)], { type: "application/json" })
      );

      // Only append image if a new one was selected
      if (editImage) {
        fd.append("image", editImage);
      }

      await productAPI.put(`/products/${editingProduct.id || editingProduct._id}`, fd, {
        headers: {
          "Content-Type": "multipart/form-data",
          "Authorization": `Bearer ${token}`
        },
      });

      console.log("✅ Product updated, reloading list...");
      await load();
      showToast("Product updated successfully!", "success");
      handleEditCancel();
    } catch (err) {
      console.error("❌ Failed to update product:", err);
      showToast("Failed to update product: " + (err.response?.data?.error || err.message), "error");
    } finally {
      setBusy(false);
    }
  }

  function handleEditImageChange(e) {
    const file = e.target.files?.[0];
    if (file) {
      setEditImage(file);
      const reader = new FileReader();
      reader.onloadend = () => setEditImagePreview(reader.result);
      reader.readAsDataURL(file);
    }
  }

  return (
    <MerchantLayout>
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={hideToast}
          duration={toast.duration}
        />
      )}
      {confirmDelete && (
        <ConfirmModal
          message={`Are you sure you want to delete "${confirmDelete.productName}"? This action cannot be undone.`}
          onConfirm={handleDeleteConfirm}
          onCancel={handleDeleteCancel}
          confirmText="Delete"
          cancelText="Cancel"
          type="danger"
        />
      )}
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-3">
          <Package size={32} className="text-[#FFD814]" />
          Product Management
        </h1>
{/*         <div className="text-sm text-gray-600 bg-white px-4 py-2 rounded-lg border border-gray-200"> */}
{/*            {email} */}
{/*          </div> */}
      </div>

      <div className="grid lg:grid-cols-2 gap-8">
        {/* Add Product Form */}
        <form
          onSubmit={handleAdd}
          className="bg-white rounded-xl shadow-md border border-gray-200 p-6 space-y-5 h-fit"
        >
          <div className="flex items-center gap-3 mb-4">
            <div className="bg-[#FFD814] p-2 rounded-lg">
              <Upload size={24} className="text-gray-900" />
            </div>
            <h2 className="text-2xl font-bold text-gray-900">Add New Product</h2>
          </div>

          {/* Product Name */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">
              Product Name <span className="text-red-500">*</span>
            </label>
            <input
              className="border border-gray-300 rounded-lg px-4 py-2.5 w-full focus:ring-2 focus:ring-[#FFD814] focus:border-transparent transition"
              placeholder="e.g., iPhone 15 Pro"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              required
            />
          </div>

          {/* Brand & Category */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">
                Brand
              </label>
              <input
                className="border border-gray-300 rounded-lg px-4 py-2.5 w-full focus:ring-2 focus:ring-[#FFD814] focus:border-transparent transition"
                placeholder="e.g., Apple"
                value={form.brand}
                onChange={(e) => setForm({ ...form, brand: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">
                Category <span className="text-red-500">*</span>
              </label>
              <select
                className="border border-gray-300 rounded-lg px-4 py-2.5 w-full focus:ring-2 focus:ring-[#FFD814] focus:border-transparent transition bg-white"
                value={form.category}
                onChange={(e) => setForm({ ...form, category: e.target.value })}
                required
              >
                <option value="">Select a category</option>
                {VALID_CATEGORIES.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Description */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">
              Description
            </label>
            <textarea
              className="border border-gray-300 rounded-lg px-4 py-2.5 w-full focus:ring-2 focus:ring-[#FFD814] focus:border-transparent transition"
              rows="3"
              placeholder="Describe your product..."
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          </div>

          {/* Attributes */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">
              Product Attributes (Optional)
            </label>
            <div className="grid grid-cols-2 gap-4">
              <input
                className="border border-gray-300 rounded-lg px-4 py-2.5 focus:ring-2 focus:ring-[#FFD814] focus:border-transparent transition"
                placeholder="Color (optional)"
                value={form.attributes.color}
                onChange={(e) =>
                  setForm({
                    ...form,
                    attributes: { ...form.attributes, color: e.target.value },
                  })
                }
              />
              <input
                className="border border-gray-300 rounded-lg px-4 py-2.5 focus:ring-2 focus:ring-[#FFD814] focus:border-transparent transition"
                placeholder="Storage (optional)"
                value={form.attributes.storage}
                onChange={(e) =>
                  setForm({
                    ...form,
                    attributes: { ...form.attributes, storage: e.target.value },
                  })
                }
              />
            </div>

          </div>

          {/* Price & Stock */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">
              Pricing & Inventory<span className="text-red-500">*</span>
            </label>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-xs text-gray-600">Price (₹)</span>
                </div>
                <input
                  className="border border-gray-300 rounded-lg px-4 py-2.5 w-full focus:ring-2 focus:ring-[#FFD814] focus:border-transparent transition [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                  type="number"
                  step="0.01"
                  min="0"
                  placeholder="Enter price"
                  value={form.merchants[0].price}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      merchants: [
                        { ...form.merchants[0], price: e.target.value === "" ? "" : Number(e.target.value) },
                      ],
                    })
                  }
                  required
                />
              </div>
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <Box size={16} className="text-blue-600" />
                  <span className="text-xs text-gray-600">Stock</span>
                </div>

                <input
                  className="border border-gray-300 rounded-lg px-4 py-2.5 w-full focus:ring-2 focus:ring-[#FFD814] focus:border-transparent transition"
                  type="number"
                  placeholder="Enter stock quantity"
                  value={form.merchants[0].stock}
                  min={0}
                  onChange={(e) => {
                    const value = e.target.value;


                    const safeValue =
                      value === "" ? "" : Math.max(0, Number(value));

                    setForm({
                      ...form,
                      merchants: [{ ...form.merchants[0], stock: safeValue }],
                    });
                  }}
                  required
                />

              </div>
            </div>
          </div>

          {/* Image Upload */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">
              Product Image <span className="text-red-500">*</span>
            </label>
            <div className="border-2 border-dashed border-gray-300 rounded-lg p-4 text-center hover:border-[#FFD814] transition">
              <input
                id="product-image"
                type="file"
                accept="image/*"
                onChange={handleImageChange}
                className="hidden"
              />
              <label
                htmlFor="product-image"
                className="cursor-pointer flex flex-col items-center gap-2"
              >
                {imagePreview ? (
                  <img
                    src={imagePreview}
                    alt="Preview"
                    className="h-32 w-32 object-cover rounded-lg"
                  />
                ) : (
                  <>
                    <Upload size={32} className="text-gray-400" />
                    <span className="text-sm text-gray-600">
                      Click to upload product image
                    </span>
                  </>
                )}
              </label>
            </div>
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            className="w-full bg-[#FFD814] hover:bg-[#F7CA00] text-gray-900 font-bold rounded-lg px-6 py-3 transition-colors disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            disabled={busy}
          >
            {busy ? (
              <>
                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-gray-900"></div>
                Uploading...
              </>
            ) : (
              <>
                <Upload size={20} />
                Add Product
              </>
            )}
          </button>
        </form>

        {/* Product List */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-2xl font-bold text-gray-900">Your Products</h2>
            <div className="text-sm text-gray-600 bg-white px-3 py-1 rounded-full border border-gray-200">
              {list.length} products
            </div>
          </div>
          <div className="space-y-4 max-h-[800px] overflow-y-auto pr-2">
            {list.length === 0 ? (
              <div className="bg-white rounded-lg border border-gray-200 p-8 text-center">
                <Package size={48} className="mx-auto text-gray-300 mb-3" />
                <p className="text-gray-600">No products yet. Add your first product!</p>
              </div>
            ) : (
              list.map((p) => (
                <div
                  key={p.id || p._id}
                  className="bg-white border border-gray-200 rounded-lg shadow-sm p-4 hover:shadow-md transition"
                >
                  <div className="flex gap-4">
                    {p.imageUrl ? (
                      <img
                        src={`${import.meta.env.VITE_PRODUCT_SERVICE_URL}${p.imageUrl}`}
                        alt={p.name}
                        className="h-24 w-24 object-cover rounded-lg flex-shrink-0"
                        onError={(e) => {
                          console.error("❌ Image failed to load:", e.target.src);
                          e.target.src = "https://via.placeholder.com/96?text=No+Image";
                        }}
                        onLoad={() => {
                          console.log("✅ Image loaded:", p.imageUrl);
                        }}
                      />
                    ) : (
                      <div className="h-24 w-24 bg-gray-200 rounded-lg flex items-center justify-center flex-shrink-0">
                        <span className="text-gray-400 text-xs">No Image</span>
                      </div>
                    )}
                    <div className="flex-1 min-w-0">
                      <h3 className="font-bold text-lg text-gray-900">{p.name}</h3>
                      <p className="text-sm text-gray-600">{p.brand} • {p.category}</p>
                      <p className="text-xs text-gray-500 mt-1 line-clamp-2">{p.description}</p>
                      <div className="flex items-center gap-4 mt-2">
                        <span className="text-sm font-semibold text-green-600">
                          ₹{p.merchants?.[0]?.price || 0}
                        </span>
                        <span className="text-sm text-gray-600">
                          Stock: {p.merchants?.[0]?.stock || 0}
                        </span>
                      </div>
                    </div>
                    <div className="flex flex-col gap-2">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleEditClick(p);
                        }}
                        className="flex items-center gap-2 px-3 py-2 bg-blue-50 hover:bg-blue-100 text-blue-600 rounded-lg transition-colors"
                        title="Edit product"
                      >
                        <Edit size={16} />
                        <span className="text-sm font-medium">Edit</span>
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDeleteClick(p.id || p._id, p.name);
                        }}
                        className="flex items-center gap-2 px-3 py-2 bg-red-50 hover:bg-red-100 text-red-600 rounded-lg transition-colors"
                        title="Delete product"
                      >
                        <Trash2 size={16} />
                        <span className="text-sm font-medium">Delete</span>
                      </button>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Edit Product Modal */}
      {editingProduct && editForm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
              <h2 className="text-2xl font-bold text-gray-900">Edit Product</h2>
              <button
                onClick={handleEditCancel}
                className="text-gray-400 hover:text-gray-600 transition-colors"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleEditSubmit} className="p-6 space-y-5">
              {/* Product Name */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Product Name *
                </label>
                <input
                  type="text"
                  value={editForm.name}
                  onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                  required
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="e.g., iPhone 15 Pro"
                />
              </div>

              {/* Brand & Category */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Brand
                  </label>
                  <input
                    type="text"
                    value={editForm.brand}
                    onChange={(e) => setEditForm({ ...editForm, brand: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="e.g., Apple"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Category *
                  </label>
                  <select
                    value={editForm.category}
                    onChange={(e) => setEditForm({ ...editForm, category: e.target.value })}
                    required
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  >
                    <option value="">Select Category</option>
                    {VALID_CATEGORIES.map(cat => (
                      <option key={cat} value={cat}>{cat}</option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Description */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Description
                </label>
                <textarea
                  value={editForm.description}
                  onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
                  rows={3}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="Product description..."
                />
              </div>

              {/* Attributes */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Color
                  </label>
                  <input
                    type="text"
                    value={editForm.attributes.color}
                    onChange={(e) => setEditForm({
                      ...editForm,
                      attributes: { ...editForm.attributes, color: e.target.value }
                    })}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="e.g., Black"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Storage
                  </label>
                  <input
                    type="text"
                    value={editForm.attributes.storage}
                    onChange={(e) => setEditForm({
                      ...editForm,
                      attributes: { ...editForm.attributes, storage: e.target.value }
                    })}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="e.g., 256GB"
                  />
                </div>
              </div>

              {/* Price & Stock */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Price (₹) *
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    value={editForm.merchants[0].price}
                    onChange={(e) => setEditForm({
                      ...editForm,
                      merchants: [{ ...editForm.merchants[0], price: e.target.value }]
                    })}
                    required
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="0.00"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Stock *
                  </label>
                  <input
                    type="number"
                    value={editForm.merchants[0].stock}
                    onChange={(e) => setEditForm({
                      ...editForm,
                      merchants: [{ ...editForm.merchants[0], stock: e.target.value }]
                    })}
                    required
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="0"
                  />
                </div>
              </div>

              {/* Image Upload */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Product Image
                </label>
                {editImagePreview && (
                  <div className="mb-3">
                    <img
                      src={editImagePreview}
                      alt="Preview"
                      className="h-32 w-32 object-cover rounded-lg border border-gray-300"
                    />
                  </div>
                )}
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleEditImageChange}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
                <p className="text-xs text-gray-500 mt-1">
                  Leave empty to keep current image
                </p>
              </div>

              {/* Action Buttons */}
              <div className="flex gap-3 pt-4">
                <button
                  type="submit"
                  disabled={busy}
                  className="flex-1 bg-[#FFD814] hover:bg-[#F7CA00] text-gray-900 font-bold py-3 px-6 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {busy ? "Updating..." : "Update Product"}
                </button>
                <button
                  type="button"
                  onClick={handleEditCancel}
                  className="px-6 py-3 border-2 border-gray-300 hover:bg-gray-50 text-gray-700 font-medium rounded-lg transition-colors"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </MerchantLayout>
  );
}

