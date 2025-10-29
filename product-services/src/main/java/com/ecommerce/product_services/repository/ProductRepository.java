package com.ecommerce.product_services.repository;

import com.ecommerce.product_services.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    @Query("{ 'category': { $regex: ?0, $options: 'i' } }")
    List<Product> findByCategory(String category);

    // Find products by exact name match (case-insensitive)
    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    List<Product> findByNameIgnoreCase(String name);
}