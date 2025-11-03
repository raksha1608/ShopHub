package com.ecommerce.merchant_service.repository;

import com.ecommerce.merchant_service.entity.MerchantProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<MerchantProduct, Long> {
    List<MerchantProduct> findByMerchantId(Long merchantId);
}
