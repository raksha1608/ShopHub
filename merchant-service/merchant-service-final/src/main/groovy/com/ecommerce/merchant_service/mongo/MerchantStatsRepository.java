package com.ecommerce.merchant_service.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface MerchantStatsRepository extends MongoRepository<MerchantStats, String> {
    Optional<MerchantStats> findByMerchantId(Long merchantId);
}

