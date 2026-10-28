package dk.campusbornholm.dtos.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CampusActivityDTO(
        String title,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String location,
        String logoUrl
) {
}
