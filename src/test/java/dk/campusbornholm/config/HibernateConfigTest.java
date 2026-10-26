package dk.campusbornholm.config;

import dk.campusbornholm.testutils.TestUtils;
import dk.campusbornholm.entities.Guess;
import dk.campusbornholm.entities.News;
import dk.campusbornholm.entities.Role;
import dk.campusbornholm.entities.User;
import dk.campusbornholm.entities.Word;
import dk.campusbornholm.entities.WordleGame;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class HibernateConfigTest {
    private static EntityManagerFactory emf;

    @BeforeAll
    static void setUp() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker kører ikke – testen springes over");
        emf = HibernateTestConfig.getEntityManagerFactory();
        TestUtils.truncateAll(emf);
    }

    @Test
    void entitiesAndRelationsCanBePersisted() {
        Long gameId;
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            Role userRole = new Role("USER");
            em.persist(userRole);

            User user = new User("elias", "hemmelig123");
            user.addRole(userRole);
            em.persist(user);

            em.persist(new News("Velkommen", "Første nyhed på campus", user));

            Word word = new Word("KAFFE");
            word.setDailyDate(LocalDate.now());
            em.persist(word);

            WordleGame game = new WordleGame(user, word);
            game.addGuess(new Guess("kaste", "GGXXG", 1));
            em.persist(game);

            em.getTransaction().commit();
            gameId = game.getId();
        }

        try (EntityManager em = emf.createEntityManager()) {
            WordleGame found = em.find(WordleGame.class, gameId);
            assertNotNull(found);
            assertEquals(1, found.getGuesses().size());
            assertEquals("kaffe", found.getWord().getWord());
            assertEquals(1, found.getUser().getRoles().size());
        }
    }
}
