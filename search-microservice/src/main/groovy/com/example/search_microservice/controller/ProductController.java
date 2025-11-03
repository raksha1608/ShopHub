package com.example.search_microservice.controller;

import com.example.search_microservice.model.Product;
import com.example.search_microservice.service.ProductService;
import com.example.search_microservice.util.InputSanitizer;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping("/add")
    public Product addProduct(@Valid @RequestBody Product product) {
        return productService.saveProduct(product);
    }

    @PostMapping("/addAll")
    public List<Product> addAllProducts(@Valid @RequestBody List<Product> products) {
        return productService.saveAll(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable("id") String id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Iterable<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<?> getProductsByCategory(@PathVariable("category") String category) {
        try {
            // Validate input length
            if (category == null || category.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Category cannot be empty"));
            }
            if (category.length() > 50) {
                return ResponseEntity.badRequest().body(Map.of("error", "Category name too long"));
            }

            List<Product> products = productService.getProductsByCategory(category);
            return ResponseEntity.ok(products);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(@RequestParam("q") String q) {
        try {
            // Validate input length
            if (q == null || q.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Search query cannot be empty"));
            }
            if (q.length() > 100) {
                return ResponseEntity.badRequest().body(Map.of("error", "Search query too long"));
            }

            List<Product> products = productService.searchProductsByName(q);
            return ResponseEntity.ok(products);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/advanced-search")
    public ResponseEntity<Map<String, Object>> advancedSearch(@Valid @RequestBody AdvancedSearchRequest request) {
        Map<String, Object> results = productService.searchProducts(
                request.getQuery(),
                request.getCategory(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getPage(),
                request.getSize(),
                request.getSortBy(),
                request.getSortOrder()
        );
        return ResponseEntity.ok(results);
    }

    @Data
    public static class AdvancedSearchRequest {
        private String query;
        private String category;
        private Double minPrice;
        private Double maxPrice;
        private int page = 0;
        private int size = 10;
        private String sortBy = "name";
        private String sortOrder = "asc";
    }
}
