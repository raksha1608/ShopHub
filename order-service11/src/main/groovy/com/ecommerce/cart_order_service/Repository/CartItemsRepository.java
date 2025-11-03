package com.ecommerce.cart_order_service.Repository;

import com.ecommerce.cart_order_service.Model.CartItems;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CartItemsRepository extends JpaRepository<CartItems, Long> {
    List<CartItems> findByUserId(Long userId);
    Optional<CartItems> findByUserIdAndProductIdAndMerchantId(Long userId, String productId, Long merchantId);
    void deleteByUserIdAndProductIdAndMerchantId(Long userId, String productId, Long merchantId);
    void deleteByUserId(Long userId);
}
