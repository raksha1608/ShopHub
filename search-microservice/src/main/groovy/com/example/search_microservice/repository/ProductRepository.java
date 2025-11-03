package com.example.search_microservice.repository;

import com.example.search_microservice.model.Product;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends ElasticsearchRepository<Product, String> {
    List<Product> findByCategory(String category);
    List<Product> findByNameContainingIgnoreCase(String name);

    @Query("{\"bool\": {\"should\": [{\"match_phrase_prefix\": {\"name\": {\"query\": \"?0\", \"boost\": 3}}}, {\"match_phrase_prefix\": {\"brand\": {\"query\": \"?0\", \"boost\": 2}}}, {\"match_phrase_prefix\": {\"category\": {\"query\": \"?0\"}}}], \"minimum_should_match\": 1}}")
    List<Product> searchByMultipleFields(String searchTerm);

}
