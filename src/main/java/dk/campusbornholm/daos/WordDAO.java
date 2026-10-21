package dk.campusbornholm.daos;

import dk.campusbornholm.dtos.WordDTO;
import dk.campusbornholm.entities.Word;
import dk.campusbornholm.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class WordDAO {
    private static final String VALID_WORD = "[a-zæøå]{5}";

    private final EntityManagerFactory emf;

    public WordDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public int saveAll(List<String> words) {
        Set<String> cleaned = words.stream()
                .map(w -> w.trim().toLowerCase())
                .filter(w -> w.matches(VALID_WORD))
                .collect(Collectors.toSet());
        if (cleaned.isEmpty()) {
            return 0;
        }

        try (EntityManager em = emf.createEntityManager()) {
            try {
                em.getTransaction().begin();
                Set<String> existing = new HashSet<>(em.createQuery(
                                "SELECT w.word FROM Word w WHERE w.word IN :words", String.class)
                        .setParameter("words", cleaned)
                        .getResultList());

                int added = 0;
                for (String w : cleaned) {
                    if (!existing.contains(w)) {
                        em.persist(new Word(w));
                        added++;
                    }
                }
                em.getTransaction().commit();
                return added;
            } catch (RuntimeException e) {
                DAOUtils.rollbackIfActive(em);
                throw e;
            }
        }
    }

    public boolean exists(String word) {
        if (word == null) {
            return false;
        }
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT COUNT(w) FROM Word w WHERE w.word = :word", Long.class)
                    .setParameter("word", word.trim().toLowerCase())
                    .getSingleResult() > 0;
        }
    }

    public long count() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT COUNT(w) FROM Word w", Long.class).getSingleResult();
        }
    }

    public synchronized WordDTO getDailyWord(LocalDate date) {
        try (EntityManager em = emf.createEntityManager()) {
            Optional<Word> existing = findByDailyDate(em, date);
            if (existing.isPresent()) {
                return new WordDTO(existing.get());
            }

            try {
                em.getTransaction().begin();
                Long unused = em.createQuery(
                                "SELECT COUNT(w) FROM Word w WHERE w.dailyDate IS NULL", Long.class)
                        .getSingleResult();
                if (unused == 0) {
                    throw new ApiException(500, "Der er ingen ubrugte ord tilbage i ordlisten");
                }

                int offset = ThreadLocalRandom.current().nextInt(unused.intValue());
                Word word = em.createQuery(
                                "SELECT w FROM Word w WHERE w.dailyDate IS NULL ORDER BY w.id", Word.class)
                        .setFirstResult(offset)
                        .setMaxResults(1)
                        .getSingleResult();
                word.setDailyDate(date);

                em.getTransaction().commit();
                return new WordDTO(word);
            } catch (RuntimeException e) {
                DAOUtils.rollbackIfActive(em);
                throw e;
            }
        }
    }

    private Optional<Word> findByDailyDate(EntityManager em, LocalDate date) {
        return em.createQuery("SELECT w FROM Word w WHERE w.dailyDate = :date", Word.class)
                .setParameter("date", date)
                .getResultStream()
                .findFirst();
    }
}
