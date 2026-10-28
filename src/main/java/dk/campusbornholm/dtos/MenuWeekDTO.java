package dk.campusbornholm.dtos;

import java.time.LocalDate;
import java.util.List;

public record MenuWeekDTO(
        int weekNumber,
        LocalDate from,
        LocalDate to,
        List<DishDTO> dishes
) {
}
