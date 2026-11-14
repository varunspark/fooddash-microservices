package com.fooddash.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "is required") String customerName,
        @NotBlank(message = "is required") String deliveryAddress,
        @NotEmpty(message = "must contain at least one item") @Valid List<Line> items) {

    public record Line(
            @NotNull(message = "is required") Long menuItemId,
            @Min(value = 1, message = "must be at least 1") @Max(value = 20, message = "must be at most 20") int quantity) {
    }
}
