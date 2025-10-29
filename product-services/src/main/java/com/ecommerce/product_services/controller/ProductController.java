package com.ecommerce.product_services.controller;

import com.ecommerce.product_services.model.Product;
import com.ecommerce.product_services.service.ProductService;
import com.ecommerce.product_services.util.AuthValidator;
import com.ecommerce.product_services.util.InputSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@CrossOrigin("*")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private AuthValidator authValidator;

    
    @PostMapping(value = "/add", consumes = "multipart/form-data")
    public ResponseEntity<?> addProduct(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestPart("product") Product product,
            @RequestPart("image") MultipartFile imageFile) {

        Map<String, Object> claims = authValidator.validateToken(authHeader);
        String role = (String) claims.get("role");

        if (!"MERCHANT".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied. Only MERCHANTS can add products."));
        }

        try {
            Product savedProduct = productService.createProduct(product, imageFile);
            return ResponseEntity.ok(savedProduct);
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Product creation failed: " + e.getMessage());
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error during product creation: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to add product: " + e.getMessage()));
        }
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping
    public ResponseEntity<?> getAllProducts(@RequestParam(required = false) String category) {
        try {
            if (category != null && !category.isEmpty()) {
                // Validate category input
                if (category.length() > 50) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Category name too long"));
                }
                if (!InputSanitizer.isValidInput(category)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid category input"));
                }

                List<Product> products = productService.getProductsByCategory(category);
                return ResponseEntity.ok(products);
            } else {
                List<Product> products = productService.getAllProducts();
                return ResponseEntity.ok(products);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

  
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<?> updateProduct(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String id,
            @RequestPart("product") Product productDetails,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        Map<String, Object> claims = authValidator.validateToken(authHeader);
        String role = (String) claims.get("role");

        if (!"MERCHANT".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied. Only MERCHANTS can update products."));
        }

        return productService.updateProduct(id, productDetails, imageFile)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String id) {

        Map<String, Object> claims = authValidator.validateToken(authHeader);
        String role = (String) claims.get("role");

        if (!"MERCHANT".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied. Only MERCHANTS can delete products."));
        }

        if (productService.deleteProduct(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

   
    @PostMapping("/updateStock")
    public ResponseEntity<?> updateStock(@RequestBody Map<String, Object> request) {
        try {
            String productId = (String) request.get("productId");

            // Handle merchantId as either String or Integer
            Object merchantIdObj = request.get("merchantId");
            Integer merchantId;
            if (merchantIdObj instanceof String) {
                merchantId = Integer.parseInt((String) merchantIdObj);
            } else if (merchantIdObj instanceof Integer) {
                merchantId = (Integer) merchantIdObj;
            } else {
                throw new IllegalArgumentException("Invalid merchantId type: " + merchantIdObj.getClass().getName());
            }

            Integer quantity = (Integer) request.get("quantity");

            System.out.println("📦 Updating stock for product: " + productId + ", merchant: " + merchantId + ", quantity: " + quantity);

            boolean success = productService.updateStock(productId, merchantId, quantity);

            if (success) {
                System.out.println("✅ Stock update successful for product: " + productId);
                return ResponseEntity.ok(Map.of("success", true, "message", "Stock updated"));
            } else {
                System.err.println("❌ Product or merchant not found: " + productId + ", merchant: " + merchantId);
                return ResponseEntity.status(404).body(Map.of("error", "Product or merchant not found"));
            }
        } catch (Exception e) {
            System.err.println("❌ Stock update failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to update stock: " + e.getMessage()));
        }
    }
}
