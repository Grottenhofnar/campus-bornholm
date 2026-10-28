package dk.campusbornholm.dtos.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CampusMenuItemDTO(
        LocalDate date,
        String title,
        String description,
        Double priceDkk
) {
}
