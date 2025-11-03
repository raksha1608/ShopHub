package com.ecommerce.cart_order_service.Repository;

import com.ecommerce.cart_order_service.Model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
}
