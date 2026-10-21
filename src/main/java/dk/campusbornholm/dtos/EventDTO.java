package dk.campusbornholm.dtos;

import dk.campusbornholm.entities.Event;

import java.time.LocalDateTime;

public record EventDTO(
        Long id,
        String title,
        String description,
        String location,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String createdBy
) {
    public EventDTO(Event event) {
        this(event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getStartTime(),
                event.getEndTime(),
                event.getCreatedBy() == null ? null : event.getCreatedBy().getUsername());
    }
}
