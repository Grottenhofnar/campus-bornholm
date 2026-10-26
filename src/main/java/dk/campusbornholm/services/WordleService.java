package dk.campusbornholm.services;

import dk.campusbornholm.config.AppClock;
import dk.campusbornholm.daos.WordDAO;
import dk.campusbornholm.daos.WordleGameDAO;
import dk.campusbornholm.dtos.WordDTO;
import dk.campusbornholm.dtos.WordleGameDTO;
import dk.campusbornholm.exceptions.ApiException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class WordleService {
    public static final int WORD_LENGTH = 5;

    public static final char GREEN = 'G';
    public static final char YELLOW = 'Y';
    public static final char GRAY = 'X';

    private final WordDAO wordDAO;
    private final WordleGameDAO gameDAO;

    public WordleService(WordDAO wordDAO, WordleGameDAO gameDAO) {
        this.wordDAO = wordDAO;
        this.gameDAO = gameDAO;
    }

    public static String evaluate(String guess, String answer) {
        String g = normalize(guess);
        String a = normalize(answer);

        char[] result = new char[WORD_LENGTH];
        Map<Character, Integer> remaining = new HashMap<>();

        for (int i = 0; i < WORD_LENGTH; i++) {
            if (g.charAt(i) == a.charAt(i)) {
                result[i] = GREEN;
            } else {
                result[i] = GRAY;
                remaining.merge(a.charAt(i), 1, Integer::sum);
            }
        }

        for (int i = 0; i < WORD_LENGTH; i++) {
            if (result[i] == GREEN) {
                continue;
            }
            char c = g.charAt(i);
            int left = remaining.getOrDefault(c, 0);
            if (left > 0) {
                result[i] = YELLOW;
                remaining.put(c, left - 1);
            }
        }

        return new String(result);
    }

    public static String normalize(String word) {
        if (word == null) {
            throw new ApiException(400, "Gættet mangler");
        }
        String w = word.trim().toLowerCase();
        if (w.length() != WORD_LENGTH) {
            throw new ApiException(400, "Gættet skal være præcis " + WORD_LENGTH + " bogstaver");
        }
        if (!w.matches("[a-zæøå]+")) {
            throw new ApiException(400, "Gættet må kun indeholde bogstaver");
        }
        return w;
    }

    public WordleGameDTO getTodaysGame(String username) {
        LocalDate today = AppClock.today();
        wordDAO.getDailyWord(today);
        return gameDAO.getOrCreate(username, today);
    }

    public WordleGameDTO makeGuess(String username, String rawGuess) {
        String guess = normalize(rawGuess);
        if (!wordDAO.exists(guess)) {
            throw new ApiException(400, "'" + guess + "' er ikke et gyldigt ord");
        }

        LocalDate today = AppClock.today();
        WordDTO daily = wordDAO.getDailyWord(today);
        WordleGameDTO game = gameDAO.getOrCreate(username, today);
        if (game.finished()) {
            throw new ApiException(400, "Du har allerede spillet dagens Wordle");
        }

        String result = evaluate(guess, daily.word());
        return gameDAO.addGuess(game.id(), guess, result);
    }
}
