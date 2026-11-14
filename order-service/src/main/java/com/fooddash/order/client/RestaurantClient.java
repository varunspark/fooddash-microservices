package com.fooddash.order.client;

import com.fooddash.order.dto.MenuItemDto;
import com.fooddash.order.dto.RestaurantDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * HTTP client for the restaurant service.
 * The base URL comes from configuration, so the same code works on a laptop
 * (http://localhost:8081) and in Docker (http://restaurant-service:8081).
 */
@Component
public class RestaurantClient {

    private final RestClient client;

    public RestaurantClient(@Value("${restaurant-service.url}") String baseUrl) {
        // Timeouts matter: without them, a hung restaurant service would hang every order request.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    /** Returns the menu item, or null if it does not exist. */
    public MenuItemDto getMenuItem(Long id) {
        return fetch("/api/menu-items/{id}", id, MenuItemDto.class);
    }

    /** Returns the restaurant, or null if it does not exist. */
    public RestaurantDto getRestaurant(Long id) {
        return fetch("/api/restaurants/{id}", id, RestaurantDto.class);
    }

    private <T> T fetch(String path, Long id, Class<T> type) {
        try {
            return client.get().uri(path, id).retrieve().body(type);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Restaurant service is unavailable right now. Please try again shortly.");
        }
    }
}
