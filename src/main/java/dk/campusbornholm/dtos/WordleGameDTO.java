package dk.campusbornholm.dtos;

import dk.campusbornholm.entities.WordleGame;

import java.time.LocalDate;
import java.util.List;

public record WordleGameDTO(
        Long id,
        String username,
        LocalDate date,
        boolean solved,
        boolean finished,
        int attempts,
        int attemptsLeft,
        List<GuessDTO> guesses,
        String word
) {
    public WordleGameDTO(WordleGame game) {
        this(game.getId(),
                game.getUser().getUsername(),
                game.getWord().getDailyDate(),
                game.isSolved(),
                game.isFinished(),
                game.getAttempts(),
                WordleGame.MAX_ATTEMPTS - game.getAttempts(),
                game.getGuesses().stream().map(GuessDTO::new).toList(),
                game.isFinished() ? game.getWord().getWord() : null);
    }
}
