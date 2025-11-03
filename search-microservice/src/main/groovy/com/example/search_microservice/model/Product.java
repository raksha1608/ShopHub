package com.example.search_microservice.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;

@Data
@Document(indexName = "products_final")
public class Product {

    @Id
    private String productId;

    @NotBlank(message = "Product name is required")
    @Field(type = FieldType.Text)
    private String name;

    @NotBlank(message = "Category is required")
    @Field(type = FieldType.Keyword)
    private String category;

    @NotBlank(message = "Brand is required")
    @Field(type = FieldType.Keyword)
    private String brand;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String imageUrl;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    private Double price;

    private List<Map<String, Object>> merchants;
}
