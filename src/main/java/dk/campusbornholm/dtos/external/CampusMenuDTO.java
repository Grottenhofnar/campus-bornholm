package dk.campusbornholm.dtos.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CampusMenuDTO(
        Integer weekNumber,
        Integer year,
        List<CampusMenuItemDTO> items
) {
}
