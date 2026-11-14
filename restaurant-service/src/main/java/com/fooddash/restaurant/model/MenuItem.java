package com.fooddash.restaurant.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Menu item. It refers to its restaurant by id only (no JPA relation),
 * which keeps the JSON simple and is how other services will see it too.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long restaurantId;

    @NotBlank(message = "must not be blank")
    private String name;

    @Column(length = 300)
    private String description;

    @NotNull(message = "is required")
    @DecimalMin(value = "1.00", message = "must be at least 1")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    private String category = "Mains";

    private boolean veg = true;

    private boolean available = true;
}
