package com.ecommerce.product_services.service;

import com.ecommerce.product_services.model.Product;
import com.ecommerce.product_services.repository.ProductRepository;
import com.ecommerce.product_services.util.InputSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private FileStorageService fileStorageService;


    public ResponseEntity<?> addProduct(Product product, MultipartFile imageFile) {
        try {
            validateProductInput(product);


            List<Product> existing = productRepository.findByNameIgnoreCase(product.getName());
            if (!existing.isEmpty()) {
                for (Product ex : existing) {
                    if (Objects.equals(ex.getBrand(), product.getBrand())) {
                        Product.Merchant newMerchant = product.getMerchants().get(0);
                        boolean exists = ex.getMerchants().stream()
                                .anyMatch(m -> m.getMerchant_id() == newMerchant.getMerchant_id());
                        if (exists)
                            throw new IllegalArgumentException("You already added this product.");
                        ex.getMerchants().add(newMerchant);
                        return ResponseEntity.ok(productRepository.save(ex));
                    }
                }
            }


            String imageUrl = fileStorageService.store(imageFile);
            product.setImageUrl(imageUrl);
            Product saved = productRepository.save(product);
            return ResponseEntity.ok(saved);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to add product"));
        }
    }


    public ResponseEntity<?> getProductByIdResponse(String id) {
        return productRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> getAllProductsResponse(String category) {
        try {
            if (category != null && !category.isEmpty()) {
                if (category.length() > 50)
                    return ResponseEntity.badRequest().body(Map.of("error", "Category name too long"));
                if (!InputSanitizer.isValidInput(category))
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid category"));

                List<Product> products = productRepository.findByCategory(category);
                return ResponseEntity.ok(products);
            } else {
                return ResponseEntity.ok(productRepository.findAll());
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch products"));
        }
    }

    public ResponseEntity<?> searchProductsResponse(String q) {
        try {
            if (q == null || q.trim().isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "Search query cannot be empty"));
            if (q.length() > 100)
                return ResponseEntity.badRequest().body(Map.of("error", "Search query too long"));
            if (!InputSanitizer.isValidInput(q))
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid query"));

            List<Product> results = productRepository.searchByMultipleFields(q);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Search failed"));
        }
    }


    public ResponseEntity<?> updateProductResponse(String id, Product details, MultipartFile imageFile) {
        return productRepository.findById(id)
                .map(existing -> {
                    existing.setName(details.getName());
                    existing.setCategory(details.getCategory());
                    existing.setBrand(details.getBrand());
                    existing.setDescription(details.getDescription());
                    existing.setAttributes(details.getAttributes());
                    existing.setMerchants(details.getMerchants());

                    if (imageFile != null && !imageFile.isEmpty()) {
                        String oldUrl = existing.getImageUrl();
                        String newUrl = fileStorageService.store(imageFile);
                        existing.setImageUrl(newUrl);
                        if (oldUrl != null) fileStorageService.delete(oldUrl);
                    }

                    return ResponseEntity.ok(productRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> deleteProductResponse(String id) {
        Optional<Product> optional = productRepository.findById(id);
        if (optional.isPresent()) {
            Product product = optional.get();
            if (product.getImageUrl() != null)
                fileStorageService.delete(product.getImageUrl());
            productRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<?> updateStockResponse(Map<String, Object> request) {
        try {
            String productId = (String) request.get("productId");
            int merchantId = Integer.parseInt(request.get("merchantId").toString());
            int quantity = (Integer) request.get("quantity");

            Optional<Product> optional = productRepository.findById(productId);
            if (optional.isEmpty())
                return ResponseEntity.status(404).body(Map.of("error", "Product not found"));

            Product product = optional.get();
            boolean updated = false;

            for (Product.Merchant m : product.getMerchants()) {
                if (m.getMerchant_id() == merchantId) {
                    m.setStock(Math.max(0, m.getStock() - quantity));
                    updated = true;
                    break;
                }
            }

            if (!updated)
                return ResponseEntity.status(404).body(Map.of("error", "Merchant not found"));

            productRepository.save(product);
            return ResponseEntity.ok(Map.of("success", true, "message", "Stock updated"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to update stock"));
        }
    }

    private void validateProductInput(Product product) {
        if (product.getName() == null || product.getName().isBlank())
            throw new IllegalArgumentException("Product name cannot be empty");

        if (!Product.VALID_CATEGORIES.contains(product.getCategory()))
            throw new IllegalArgumentException("Invalid category");
    }
}











