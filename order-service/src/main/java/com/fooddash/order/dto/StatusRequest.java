package com.fooddash.order.dto;

import com.fooddash.order.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull(message = "is required") OrderStatus status) {
}
