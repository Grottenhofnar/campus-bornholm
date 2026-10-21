package dk.campusbornholm.dtos;

import dk.campusbornholm.entities.News;

import java.time.LocalDateTime;

public record NewsDTO(
        Long id,
        String title,
        String content,
        LocalDateTime createdAt,
        String authorName
) {
    public NewsDTO(News news) {
        this(news.getId(),
                news.getTitle(),
                news.getContent(),
                news.getCreatedAt(),
                news.getAuthor().getUsername());
    }
}
