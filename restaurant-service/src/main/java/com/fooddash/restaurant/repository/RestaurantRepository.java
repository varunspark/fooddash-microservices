package com.fooddash.restaurant.repository;

import com.fooddash.restaurant.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    /** Matches restaurant name, cuisine, or dish name. An empty query returns everything. */
    @Query("""
            select r from Restaurant r
            where lower(r.name) like lower(concat('%', :q, '%'))
               or lower(r.cuisine) like lower(concat('%', :q, '%'))
               or r.id in (select m.restaurantId from MenuItem m
                           where lower(m.name) like lower(concat('%', :q, '%')))
            order by r.rating desc
            """)
    List<Restaurant> search(@Param("q") String q);
}
