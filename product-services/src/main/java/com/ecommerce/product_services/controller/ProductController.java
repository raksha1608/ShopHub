package com.ecommerce.product_services.controller;

import com.ecommerce.product_services.model.Product;
import com.ecommerce.product_services.service.ProductService;
import com.ecommerce.product_services.util.AuthValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/products")
@CrossOrigin("*")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private AuthValidator authValidator;

    private boolean isMerchant(String authHeader) {
        Map<String, Object> claims = authValidator.validateToken(authHeader);
        String role = (String) claims.get("role");
        return "MERCHANT".equalsIgnoreCase(role);
    }

    @PostMapping(value = "/add", consumes = "multipart/form-data")
    public ResponseEntity<?> addProduct(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestPart("product") Product product,
            @RequestPart("image") MultipartFile imageFile) {

        if (!isMerchant(authHeader)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Access denied. Only MERCHANTS can add products."));
        }

        return productService.addProduct(product, imageFile);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable String id) {
        return productService.getProductByIdResponse(id);
    }

    @GetMapping
    public ResponseEntity<?> getAllProducts(@RequestParam(required = false) String category) {
        return productService.getAllProductsResponse(category);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(@RequestParam("q") String q) {
        return productService.searchProductsResponse(q);
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<?> updateProduct(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String id,
            @RequestPart("product") Product productDetails,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        if (!isMerchant(authHeader)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Access denied. Only MERCHANTS can update products."));
        }

        return productService.updateProductResponse(id, productDetails, imageFile);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String id) {

        if (!isMerchant(authHeader)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Access denied. Only MERCHANTS can delete products."));
        }

        return productService.deleteProductResponse(id);
    }

    @PostMapping("/updateStock")
    public ResponseEntity<?> updateStock(@RequestBody Map<String, Object> request) {
        return productService.updateStockResponse(request);
    }
}











