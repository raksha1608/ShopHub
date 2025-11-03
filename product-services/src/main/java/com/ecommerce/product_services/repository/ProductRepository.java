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

    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    List<Product> findByNameIgnoreCase(String name);
    
    @Query("{ $or: [ " +
           "{ 'name': { $regex: ?0, $options: 'i' } }, " +
           "{ 'brand': { $regex: ?0, $options: 'i' } }, " +
           "{ 'category': { $regex: ?0, $options: 'i' } }, " +
           "{ 'description': { $regex: ?0, $options: 'i' } } " +
           "] }")
    List<Product> searchByMultipleFields(String searchTerm);
}