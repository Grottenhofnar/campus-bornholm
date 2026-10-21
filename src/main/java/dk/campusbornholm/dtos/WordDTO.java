package dk.campusbornholm.dtos;

import dk.campusbornholm.entities.Word;

import java.time.LocalDate;

public record WordDTO(
        Long id,
        String word,
        LocalDate dailyDate
) {
    public WordDTO(Word word) {
        this(word.getId(), word.getWord(), word.getDailyDate());
    }
}
