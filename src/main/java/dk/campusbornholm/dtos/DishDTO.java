package dk.campusbornholm.dtos;

import dk.campusbornholm.entities.Dish;

import java.time.LocalDate;

public record DishDTO(
        Long id,
        String name,
        String description,
        Double price,
        LocalDate date,
        String sourceUrl
) {
    public DishDTO(Dish dish) {
        this(dish.getId(),
                dish.getName(),
                dish.getDescription(),
                dish.getPrice(),
                dish.getDate(),
                dish.getSourceUrl());
    }
}
