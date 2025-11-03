package com.ecommerce.merchant_service.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "merchant_stats")
public class MerchantStats {
    @Id
    private String id;
    private Long merchantId;
    private int productsOfferedCount;
    private int productsSoldCount;
    private double avgRating;
    private List<Map<String, Object>> reviews;
    private String updatedAt;
}
