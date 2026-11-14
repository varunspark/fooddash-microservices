package com.fooddash.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** The part of the restaurant service's menu item that the order service cares about. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MenuItemDto(Long id, Long restaurantId, String name, BigDecimal price, boolean available) {
}
