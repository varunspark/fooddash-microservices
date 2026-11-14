package com.fooddash.order.repository;

import com.fooddash.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerNameIgnoreCaseOrderByCreatedAtDesc(String customerName);

    List<Order> findAllByOrderByCreatedAtDesc();
}
