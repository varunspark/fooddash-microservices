package com.fooddash.restaurant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RestaurantApiTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void listsSeededRestaurants() throws Exception {
        mvc.perform(get("/api/restaurants"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pizza Palace")));
    }

    @Test
    void searchMatchesDishNames() throws Exception {
        mvc.perform(get("/api/restaurants").param("q", "salan"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Spice Route Biryani")));
    }

    @Test
    void exposesMenuItemForOtherServices() throws Exception {
        mvc.perform(get("/api/menu-items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantId").exists())
                .andExpect(jsonPath("$.price").exists());
    }

    @Test
    void unknownRestaurantReturns404() throws Exception {
        mvc.perform(get("/api/restaurants/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
