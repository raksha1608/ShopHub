package com.example.search_microservice.service;

import com.example.search_microservice.model.Product;
import com.example.search_microservice.repository.ProductRepository;
import com.example.search_microservice.util.InputSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    public List<Product> saveAll(List<Product> products) {
        return (List<Product>) productRepository.saveAll(products);
    }

    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    public List<Product> getProductsByCategory(String category) {
        // Sanitize category input to prevent injection
        String sanitizedCategory = InputSanitizer.sanitizeForSearch(category);

        // Validate input
        if (!InputSanitizer.isValidInput(category)) {
            throw new IllegalArgumentException("Invalid category input detected");
        }

        return productRepository.findByCategory(sanitizedCategory);
    }

    public List<Product> searchProductsByName(String name) {
        // Sanitize search query to prevent injection
        String sanitizedName = InputSanitizer.sanitizeForSearch(name);

        // Validate input
        if (!InputSanitizer.isValidInput(name)) {
            throw new IllegalArgumentException("Invalid search query detected");
        }

        // Use multi-field search to search across name, brand, description, and category
        return productRepository.searchByMultipleFields(sanitizedName);
    }

    public Iterable<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * Advanced search with optional filters, pagination, sorting
     */
    public Map<String, Object> searchProducts(String query,
                                              String category,
                                              Double minPrice,
                                              Double maxPrice,
                                              int page,
                                              int size,
                                              String sortBy,
                                              String sortOrder) {

        // Sanitize and validate query input
        String sanitizedQuery = "";
        if (query != null && !query.isEmpty()) {
            if (!InputSanitizer.isValidInput(query)) {
                throw new IllegalArgumentException("Invalid search query detected");
            }
            sanitizedQuery = InputSanitizer.sanitizeForSearch(query);
        }

        // Fetch all products matching query using multi-field search
        List<Product> allProducts = productRepository.searchByMultipleFields(sanitizedQuery);

        // Filter by category
        if (category != null && !category.isEmpty()) {
            // Sanitize category input
            if (!InputSanitizer.isValidInput(category)) {
                throw new IllegalArgumentException("Invalid category input detected");
            }
            String sanitizedCategory = InputSanitizer.sanitizeCategory(category);
            allProducts.removeIf(p -> !p.getCategory().equalsIgnoreCase(sanitizedCategory));
        }

        // Filter by price
        if (minPrice != null) {
            allProducts.removeIf(p -> p.getPrice() < minPrice);
        }
        if (maxPrice != null) {
            allProducts.removeIf(p -> p.getPrice() > maxPrice);
        }

        // Sort
        allProducts.sort((p1, p2) -> {
            int result = 0;
            switch (sortBy) {
                case "name":
                    result = p1.getName().compareToIgnoreCase(p2.getName());
                    break;
                case "price":
                    result = Double.compare(p1.getPrice(), p2.getPrice());
                    break;
                default:
                    result = p1.getName().compareToIgnoreCase(p2.getName());
            }
            return sortOrder.equalsIgnoreCase("desc") ? -result : result;
        });

        // Pagination
        int start = page * size;
        int end = Math.min(start + size, allProducts.size());
        List<Product> pagedProducts = start < end ? allProducts.subList(start, end) : List.of();

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("total", allProducts.size());
        response.put("page", page);
        response.put("size", size);
        response.put("products", pagedProducts);

        return response;
    }
}
