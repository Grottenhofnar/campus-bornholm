package dk.campusbornholm;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

public final class TestUtils {
    private TestUtils() {
    }

    public static void truncateAll(EntityManagerFactory emf) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("""
                    TRUNCATE TABLE guesses, wordle_games, words, news, events, dishes,
                                   user_roles, users, roles
                    RESTART IDENTITY CASCADE
                    """).executeUpdate();
            em.getTransaction().commit();
        }
    }
}
