package dk.campusbornholm.dtos;

import dk.campusbornholm.entities.Guess;

public record GuessDTO(
        String guess,
        String result,
        int attemptNumber
) {
    public GuessDTO(Guess guess) {
        this(guess.getGuessText(), guess.getResult(), guess.getAttemptNumber());
    }
}
