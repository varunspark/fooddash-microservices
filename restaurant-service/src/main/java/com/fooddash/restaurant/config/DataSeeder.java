package com.fooddash.restaurant.config;

import com.fooddash.restaurant.model.MenuItem;
import com.fooddash.restaurant.model.Restaurant;
import com.fooddash.restaurant.repository.MenuItemRepository;
import com.fooddash.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Loads demo restaurants and menus on first start (only when the database is empty). */
@Component
@ConditionalOnProperty(name = "fooddash.seed", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RestaurantRepository restaurants;
    private final MenuItemRepository menuItems;

    @Override
    public void run(String... args) {
        if (restaurants.count() > 0) {
            return;
        }

        Restaurant pizza = restaurant("Pizza Palace", "Italian, Pizza", "🍕",
                "Wood-fired pizzas and creamy pastas.", 4.5, 30);
        item(pizza, "Starters", "Garlic Bread", "Toasted with herb butter", "129", true);
        item(pizza, "Pizzas", "Margherita", "Tomato, mozzarella, basil", "299", true);
        item(pizza, "Pizzas", "Pepperoni Feast", "Double pepperoni, extra cheese", "449", false);
        item(pizza, "Desserts", "Choco Lava Cake", "Warm centre, served with dip", "119", true);

        Restaurant biryani = restaurant("Spice Route Biryani", "Biryani, Hyderabadi", "🍛",
                "Dum-cooked biryanis with slow-roasted spices.", 4.4, 35);
        item(biryani, "Biryani", "Chicken Dum Biryani", "Basmati, tender chicken, raita", "299", false);
        item(biryani, "Biryani", "Paneer Biryani", "Marinated paneer, fragrant rice", "259", true);
        item(biryani, "Sides", "Mirchi Ka Salan", "Peanut and sesame chilli curry", "89", true);

        Restaurant dosa = restaurant("Dosa Corner", "South Indian", "🥞",
                "Crisp dosas, fluffy idlis and filter coffee.", 4.6, 25);
        item(dosa, "Tiffin", "Masala Dosa", "Potato masala, chutneys, sambar", "99", true);
        item(dosa, "Tiffin", "Ghee Podi Idli", "Four idlis tossed in gunpowder", "89", true);
        item(dosa, "Beverages", "Filter Coffee", "Strong and frothy", "45", true);

        Restaurant burger = restaurant("Burger Barn", "Burgers, Fast Food", "🍔",
                "Smash burgers, loaded fries and thick shakes.", 4.2, 25);
        item(burger, "Burgers", "Classic Veg Burger", "Crispy patty, lettuce, special sauce", "149", true);
        item(burger, "Burgers", "Double Smash Chicken", "Two patties, cheddar, pickles", "269", false);
        item(burger, "Sides", "Peri Peri Fries", "Spicy and salty", "109", true);

        Restaurant noodle = restaurant("Noodle House", "Chinese, Asian", "🍜",
                "Wok-tossed noodles, momos and rice bowls.", 4.3, 30);
        item(noodle, "Starters", "Veg Steamed Momos", "Six pieces with chilli dip", "119", true);
        item(noodle, "Mains", "Hakka Noodles", "Wok-tossed with crunchy veggies", "179", true);
        item(noodle, "Mains", "Schezwan Fried Rice", "Fiery and fragrant", "199", true);

        Restaurant sweet = restaurant("Sweet Tooth", "Desserts, Ice Cream", "🍰",
                "Cakes, waffles and ice cream for the late-night craving.", 4.7, 20);
        item(sweet, "Cakes", "Red Velvet Slice", "Cream cheese frosting", "149", true);
        item(sweet, "Waffles", "Nutella Waffle", "Crisp waffle, hazelnut spread", "169", true);
        item(sweet, "Ice Cream", "Butterscotch Sundae", "Crunchy praline topping", "129", true);
    }

    private Restaurant restaurant(String name, String cuisine, String emoji, String description,
                                  double rating, int mins) {
        Restaurant r = new Restaurant();
        r.setName(name);
        r.setCuisine(cuisine);
        r.setEmoji(emoji);
        r.setDescription(description);
        r.setRating(rating);
        r.setDeliveryTimeMins(mins);
        return restaurants.save(r);
    }

    private void item(Restaurant r, String category, String name, String description,
                      String price, boolean veg) {
        MenuItem m = new MenuItem();
        m.setRestaurantId(r.getId());
        m.setCategory(category);
        m.setName(name);
        m.setDescription(description);
        m.setPrice(new BigDecimal(price));
        m.setVeg(veg);
        menuItems.save(m);
    }
}
