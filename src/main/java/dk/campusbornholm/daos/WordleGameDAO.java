package dk.campusbornholm.daos;

import dk.campusbornholm.dtos.LeaderboardEntryDTO;
import dk.campusbornholm.dtos.WordleGameDTO;
import dk.campusbornholm.entities.Guess;
import dk.campusbornholm.entities.User;
import dk.campusbornholm.entities.Word;
import dk.campusbornholm.entities.WordleGame;
import dk.campusbornholm.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class WordleGameDAO {
    private final EntityManagerFactory emf;

    public WordleGameDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public WordleGameDTO getOrCreate(String username, LocalDate date) {
        try (EntityManager em = emf.createEntityManager()) {
            Optional<WordleGame> existing = find(em, username, date);
            if (existing.isPresent()) {
                return new WordleGameDTO(existing.get());
            }

            try {
                em.getTransaction().begin();
                User user = DAOUtils.findUserOrThrow(em, username);
                Word word = em.createQuery("SELECT w FROM Word w WHERE w.dailyDate = :date", Word.class)
                        .setParameter("date", date)
                        .getResultStream()
                        .findFirst()
                        .orElseThrow(() -> new ApiException(404, "Der er ikke valgt et ord for " + date));

                WordleGame game = new WordleGame(user, word);
                em.persist(game);
                em.getTransaction().commit();
                return new WordleGameDTO(game);
            } catch (RuntimeException e) {
                DAOUtils.rollbackIfActive(em);
                throw e;
            }
        }
    }

    public WordleGameDTO getByUserAndDate(String username, LocalDate date) {
        try (EntityManager em = emf.createEntityManager()) {
            return find(em, username, date)
                    .map(WordleGameDTO::new)
                    .orElseThrow(() -> new ApiException(404, username + " har ikke et spil for " + date));
        }
    }

    public WordleGameDTO addGuess(Long gameId, String guessText, String result) {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                em.getTransaction().begin();
                WordleGame game = em.find(WordleGame.class, gameId);
                if (game == null) {
                    throw new ApiException(404, "Spil med id " + gameId + " findes ikke");
                }
                if (game.isFinished()) {
                    throw new ApiException(400, "Spillet er allerede slut");
                }

                game.addGuess(new Guess(guessText, result, game.getAttempts() + 1));
                if (result.chars().allMatch(c -> c == 'G')) {
                    game.setSolved(true);
                }

                em.getTransaction().commit();
                return new WordleGameDTO(game);
            } catch (RuntimeException e) {
                DAOUtils.rollbackIfActive(em);
                throw e;
            }
        }
    }

    public List<LeaderboardEntryDTO> getLeaderboard(int limit) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                            SELECT new dk.campusbornholm.dtos.LeaderboardEntryDTO(u.username, COUNT(g))
                            FROM WordleGame g
                            JOIN g.user u
                            WHERE g.solved = true
                            GROUP BY u.username
                            ORDER BY COUNT(g) DESC, u.username
                            """, LeaderboardEntryDTO.class)
                    .setMaxResults(limit)
                    .getResultList();
        }
    }

    private Optional<WordleGame> find(EntityManager em, String username, LocalDate date) {
        return em.createQuery("""
                        SELECT g FROM WordleGame g
                        JOIN FETCH g.user u
                        JOIN FETCH g.word w
                        LEFT JOIN FETCH g.guesses
                        WHERE u.username = :username AND w.dailyDate = :date
                        """, WordleGame.class)
                .setParameter("username", username)
                .setParameter("date", date)
                .getResultStream()
                .findFirst();
    }
}
