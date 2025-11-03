package com.ecommerce.product_services.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@Document(collection = "productsNew")
public class Product {

    @Id
    private String id;
    private String name;
    private String category;
    private String brand;
    private String description;
    private java.util.Map<String, String> attributes;
    private List<Merchant> merchants;
    private String imageUrl;

    public static final List<String> VALID_CATEGORIES = List.of(
        "Electronics",
        "Fashion",
        "Home & Kitchen",
        "Beauty & Personal Care",
        "Sports & Outdoors",
        "Books & Stationery",
        "Toys & Baby Products"
    );

   /* @Data
    public static class Attributes {
        private String storage;
        private String color;
    } */

    @Data
    public static class Merchant {
        private int merchant_id;
        private String name;
        private double price;
        private int stock;
    }
}