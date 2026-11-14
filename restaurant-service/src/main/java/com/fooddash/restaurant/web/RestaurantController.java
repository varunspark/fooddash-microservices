package com.fooddash.restaurant.web;

import com.fooddash.restaurant.model.MenuItem;
import com.fooddash.restaurant.model.Restaurant;
import com.fooddash.restaurant.repository.MenuItemRepository;
import com.fooddash.restaurant.repository.RestaurantRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantRepository restaurants;
    private final MenuItemRepository menuItems;

    @GetMapping("/restaurants")
    public List<Restaurant> list(@RequestParam(defaultValue = "") String q) {
        return restaurants.search(q.trim());
    }

    @GetMapping("/restaurants/{id}")
    public Restaurant get(@PathVariable Long id) {
        return restaurants.findById(id)
                .orElseThrow(() -> notFound("Restaurant " + id + " not found"));
    }

    @GetMapping("/restaurants/{id}/menu")
    public List<MenuItem> menu(@PathVariable Long id) {
        ensureRestaurantExists(id);
        return menuItems.findByRestaurantIdOrderByIdAsc(id);
    }

    /** Internal endpoint: the order service calls this to get the real price of an item. */
    @GetMapping("/menu-items/{id}")
    public MenuItem menuItem(@PathVariable Long id) {
        return menuItems.findById(id)
                .orElseThrow(() -> notFound("Menu item " + id + " not found"));
    }

    @PostMapping("/restaurants")
    @ResponseStatus(HttpStatus.CREATED)
    public Restaurant create(@Valid @RequestBody Restaurant restaurant) {
        restaurant.setId(null);
        return restaurants.save(restaurant);
    }

    @PostMapping("/restaurants/{id}/menu")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItem addItem(@PathVariable Long id, @Valid @RequestBody MenuItem item) {
        ensureRestaurantExists(id);
        item.setId(null);
        item.setRestaurantId(id);
        return menuItems.save(item);
    }

    private void ensureRestaurantExists(Long id) {
        if (!restaurants.existsById(id)) {
            throw notFound("Restaurant " + id + " not found");
        }
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
