package com.fooddash.order.dto;

import com.fooddash.order.model.Order;
import com.fooddash.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String customerName,
        String deliveryAddress,
        Long restaurantId,
        String restaurantName,
        List<Item> items,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal total,
        OrderStatus status,
        LocalDateTime createdAt) {

    public record Item(Long menuItemId, String name, BigDecimal price, int quantity, BigDecimal lineTotal) {
    }

    public static OrderResponse from(Order o) {
        List<Item> items = o.getItems().stream()
                .map(i -> new Item(i.getMenuItemId(), i.getName(), i.getPrice(), i.getQuantity(), i.getLineTotal()))
                .toList();
        return new OrderResponse(o.getId(), o.getCustomerName(), o.getDeliveryAddress(),
                o.getRestaurantId(), o.getRestaurantName(), items,
                o.getSubtotal(), o.getDeliveryFee(), o.getTotal(), o.getStatus(), o.getCreatedAt());
    }
}
