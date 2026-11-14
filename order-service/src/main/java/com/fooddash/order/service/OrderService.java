package com.fooddash.order.service;

import com.fooddash.order.client.RestaurantClient;
import com.fooddash.order.dto.CreateOrderRequest;
import com.fooddash.order.dto.MenuItemDto;
import com.fooddash.order.dto.RestaurantDto;
import com.fooddash.order.model.Order;
import com.fooddash.order.model.OrderItem;
import com.fooddash.order.model.OrderStatus;
import com.fooddash.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final RestaurantClient restaurants;
    private final BigDecimal deliveryFee;

    public OrderService(OrderRepository orders, RestaurantClient restaurants,
                        @Value("${fooddash.delivery-fee:30}") BigDecimal deliveryFee) {
        this.orders = orders;
        this.restaurants = restaurants;
        this.deliveryFee = deliveryFee;
    }

    /**
     * The remote calls happen first and the database write happens last, on purpose:
     * we never hold a database transaction open while waiting on another service.
     * Prices always come from the restaurant service, never from the client.
     */
    public Order create(CreateOrderRequest request) {
        Long restaurantId = null;
        BigDecimal subtotal = BigDecimal.ZERO;
        Order order = new Order();
        order.setCustomerName(request.customerName().trim());
        order.setDeliveryAddress(request.deliveryAddress().trim());

        for (CreateOrderRequest.Line line : request.items()) {
            MenuItemDto menuItem = restaurants.getMenuItem(line.menuItemId());
            if (menuItem == null) {
                throw badRequest("Menu item " + line.menuItemId() + " does not exist.");
            }
            if (!menuItem.available()) {
                throw badRequest(menuItem.name() + " is sold out.");
            }
            if (restaurantId == null) {
                restaurantId = menuItem.restaurantId();
            } else if (!restaurantId.equals(menuItem.restaurantId())) {
                throw badRequest("All items in an order must come from one restaurant.");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setMenuItemId(menuItem.id());
            orderItem.setName(menuItem.name());
            orderItem.setPrice(menuItem.price());
            orderItem.setQuantity(line.quantity());
            order.addItem(orderItem);
            subtotal = subtotal.add(orderItem.getLineTotal());
        }

        RestaurantDto restaurant = restaurants.getRestaurant(restaurantId);
        if (restaurant == null) {
            throw badRequest("Restaurant " + restaurantId + " does not exist.");
        }

        order.setRestaurantId(restaurantId);
        order.setRestaurantName(restaurant.name());
        order.setSubtotal(subtotal);
        order.setDeliveryFee(deliveryFee);
        order.setTotal(subtotal.add(deliveryFee));
        return orders.save(order);
    }

    public Order get(Long id) {
        return orders.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order " + id + " not found"));
    }

    public List<Order> list(String customerName) {
        if (customerName == null || customerName.isBlank()) {
            return orders.findAllByOrderByCreatedAtDesc();
        }
        return orders.findByCustomerNameIgnoreCaseOrderByCreatedAtDesc(customerName.trim());
    }

    public Order updateStatus(Long id, OrderStatus status) {
        Order order = get(id);
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order is already " + order.getStatus() + " and can no longer change.");
        }
        order.setStatus(status);
        return orders.save(order);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
