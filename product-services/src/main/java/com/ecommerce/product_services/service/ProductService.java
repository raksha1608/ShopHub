package com.ecommerce.product_services.service;

import com.ecommerce.product_services.model.Product;
import com.ecommerce.product_services.repository.ProductRepository;
import com.ecommerce.product_services.util.InputSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private FileStorageService fileStorageService;


    public Product createProduct(Product product, MultipartFile imageFile) {
        System.out.println("📦 Creating product: " + product.getName());

        // Sanitize product name
        String sanitizedName = InputSanitizer.sanitizeProductName(product.getName());
        if (sanitizedName.isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }
        product.setName(sanitizedName);

        // Validate and sanitize category
        String sanitizedCategory = InputSanitizer.sanitizeCategory(product.getCategory());
        if (sanitizedCategory.isEmpty() || !Product.VALID_CATEGORIES.contains(sanitizedCategory)) {
            throw new IllegalArgumentException("Invalid category. Must be one of: " + String.join(", ", Product.VALID_CATEGORIES));
        }
        product.setCategory(sanitizedCategory);

        // Check if product with same name already exists
        String safeNameForQuery = InputSanitizer.sanitizeForRegex(product.getName());
        List<Product> existingProducts = productRepository.findByNameIgnoreCase(safeNameForQuery);

        if (!existingProducts.isEmpty()) {
            // Product exists - check if it's truly the same product (same name and brand only)
            for (Product existing : existingProducts) {
                boolean sameBrand = existing.getBrand() != null &&
                                   existing.getBrand().equalsIgnoreCase(product.getBrand());

                if (sameBrand) {
                    System.out.println("✅ Found existing product with same name and brand. Checking merchant...");

                    // Add new merchant to existing product
                    if (product.getMerchants() != null && !product.getMerchants().isEmpty()) {
                        Product.Merchant newMerchant = product.getMerchants().get(0);

                        // Check if merchant already exists for this product
                        boolean merchantExists = existing.getMerchants().stream()
                            .anyMatch(m -> m.getMerchant_id() == newMerchant.getMerchant_id());

                        if (merchantExists) {
                            // Merchant already sells this exact product - reject the request
                            System.out.println("❌ Merchant " + newMerchant.getMerchant_id() + " already sells this product.");
                            throw new IllegalArgumentException("You have already added this product. You cannot add the same product twice.");
                        } else {
                            System.out.println("✅ Adding new merchant " + newMerchant.getMerchant_id() + " to existing product.");
                            existing.getMerchants().add(newMerchant);
                        }

                        return productRepository.save(existing);
                    }
                }
            }
        }

        // No matching product found - create new product
        System.out.println("📦 Creating new product entry.");
        String imageUrl = fileStorageService.store(imageFile);
        product.setImageUrl(imageUrl);
        return productRepository.save(product);
    }

    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }


    public List<Product> getProductsByCategory(String category) {
        // Sanitize category input to prevent NoSQL injection
        String sanitizedCategory = InputSanitizer.sanitizeForRegex(category);

        // Validate input
        if (!InputSanitizer.isValidInput(category)) {
            throw new IllegalArgumentException("Invalid category input detected");
        }

        return productRepository.findByCategory(sanitizedCategory);
    }


    public Optional<Product> updateProduct(String id, Product productDetails, MultipartFile imageFile) {
        return productRepository.findById(id)
                .map(existingProduct -> {
                  
                    existingProduct.setName(productDetails.getName());
                    existingProduct.setCategory(productDetails.getCategory());
                    existingProduct.setBrand(productDetails.getBrand());
                    existingProduct.setDescription(productDetails.getDescription());
                    existingProduct.setAttributes(productDetails.getAttributes());
                    existingProduct.setMerchants(productDetails.getMerchants());

                  
                    if (imageFile != null && !imageFile.isEmpty()) {
                
                        String oldImageUrl = existingProduct.getImageUrl();

                       
                        String newImageUrl = fileStorageService.store(imageFile);
                        existingProduct.setImageUrl(newImageUrl);

                        if (oldImageUrl != null) {
                            fileStorageService.delete(oldImageUrl);
                        }
                    }

                    return productRepository.save(existingProduct);
                });
    }


    public boolean deleteProduct(String id) {
        Optional<Product> productOptional = productRepository.findById(id);
        if (productOptional.isPresent()) {
            Product product = productOptional.get();
            String imageUrl = product.getImageUrl();

            if (imageUrl != null) {
                fileStorageService.delete(imageUrl);
            }

            productRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean updateStock(String productId, Integer merchantId, Integer quantity) {
        Optional<Product> productOptional = productRepository.findById(productId);

        if (productOptional.isEmpty()) {
            System.err.println("❌ Product not found: " + productId);
            return false;
        }

        Product product = productOptional.get();
        List<Product.Merchant> merchants = product.getMerchants();

        if (merchants == null || merchants.isEmpty()) {
            System.err.println("❌ No merchants found for product: " + productId);
            return false;
        }

        
        boolean merchantFound = false;
        for (Product.Merchant merchant : merchants) {
            if (merchant.getMerchant_id() == merchantId) {
                int currentStock = merchant.getStock();
                int newStock = currentStock - quantity;

                if (newStock < 0) {
                    System.err.println("⚠️ Warning: Stock would go negative for product " + productId + ", merchant " + merchantId);
                    newStock = 0; 
                }

                merchant.setStock(newStock);
                merchantFound = true;
                System.out.println("✅ Updated stock for product " + productId + ", merchant " + merchantId + ": " + currentStock + " -> " + newStock);
                break;
            }
        }

        if (!merchantFound) {
            System.err.println("❌ Merchant " + merchantId + " not found for product " + productId);
            return false;
        }

        productRepository.save(product);
        return true;
    }
}