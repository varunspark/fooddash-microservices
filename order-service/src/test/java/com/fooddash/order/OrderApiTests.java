package com.fooddash.order;

import com.fooddash.order.client.RestaurantClient;
import com.fooddash.order.dto.MenuItemDto;
import com.fooddash.order.dto.RestaurantDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The restaurant service is replaced by a mock, so these tests need no other service running. */
@SpringBootTest
@AutoConfigureMockMvc
class OrderApiTests {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private RestaurantClient restaurants;

    private static final String VALID_ORDER = """
            {"customerName":"Asha","deliveryAddress":"HSR Layout","items":[{"menuItemId":1,"quantity":2}]}
            """;

    @Test
    void createsOrderUsingPricesFromRestaurantService() throws Exception {
        when(restaurants.getMenuItem(1L))
                .thenReturn(new MenuItemDto(1L, 10L, "Margherita", new BigDecimal("299.00"), true));
        when(restaurants.getRestaurant(10L)).thenReturn(new RestaurantDto(10L, "Pizza Palace"));

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.restaurantName").value("Pizza Palace"))
                .andExpect(jsonPath("$.subtotal").value(598.0))
                .andExpect(jsonPath("$.total").value(628.0))
                .andExpect(jsonPath("$.status").value("PLACED"));
    }

    @Test
    void rejectsUnknownMenuItem() throws Exception {
        when(restaurants.getMenuItem(1L)).thenReturn(null);

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void returns503WhenRestaurantServiceIsDown() throws Exception {
        when(restaurants.getMenuItem(1L))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "down"));

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void rejectsEmptyOrders() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"Asha\",\"deliveryAddress\":\"HSR\",\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsOrders() throws Exception {
        mvc.perform(get("/api/orders").param("customerName", "nobody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
