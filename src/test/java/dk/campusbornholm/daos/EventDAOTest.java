package dk.campusbornholm.daos;

import dk.campusbornholm.TestUtils;
import dk.campusbornholm.config.HibernateConfig;
import dk.campusbornholm.dtos.EventDTO;
import dk.campusbornholm.entities.Event;
import dk.campusbornholm.entities.User;
import dk.campusbornholm.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class EventDAOTest {
    private static final String ADMIN = "testadmin";

    private static EntityManagerFactory emf;
    private static EventDAO dao;

    private Event fredagsbar;
    private Event hackathon;
    private Event julefrokost;
    private Event gammeltEvent;

    @BeforeAll
    static void setUpAll() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker kører ikke – testen springes over");
        emf = HibernateConfig.getEntityManagerFactoryForTest();
        dao = new EventDAO(emf);
    }

    @BeforeEach
    void setUp() {
        TestUtils.truncateAll(emf);

        LocalDateTime now = LocalDateTime.now();
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            User admin = new User(ADMIN, "test123");
            em.persist(admin);

            fredagsbar = new Event("Fredagsbar", "Øl og musik", "Kantinen",
                    now.plusDays(1), now.plusDays(1).plusHours(4), admin);
            hackathon = new Event("Hackathon", "24 timers kodning", "Lokale 2.10",
                    now.plusDays(13), now.plusDays(14), admin);
            julefrokost = new Event("Julefrokost", "Årets fest", "Kantinen",
                    now.plusDays(20), now.plusDays(20).plusHours(6), admin);
            gammeltEvent = new Event("Gammelt event", "Er allerede sket", "Aulaen",
                    now.minusDays(1), now.minusDays(1).plusHours(2), null);

            em.persist(fredagsbar);
            em.persist(hackathon);
            em.persist(julefrokost);
            em.persist(gammeltEvent);

            em.getTransaction().commit();
        }
    }

    @AfterAll
    static void tearDownAll() {
        if (emf != null) {
            TestUtils.truncateAll(emf);
        }
    }

    @Test
    @DisplayName("create gemmer et event og returnerer det med id og opretter")
    void create() {
        LocalDateTime start = LocalDateTime.now().plusDays(5);
        EventDTO input = new EventDTO(null, "Brætspilsaften", "Medbring spil", "Loungen",
                start, start.plusHours(3), ADMIN);

        EventDTO result = dao.create(input);

        assertNotNull(result.id());
        assertEquals("Brætspilsaften", result.title());
        assertEquals(ADMIN, result.createdBy());
        assertEquals(5, dao.getAll().size());
    }

    @Test
    @DisplayName("create uden opretter er tilladt")
    void createWithoutCreator() {
        EventDTO input = new EventDTO(null, "Åbent hus", null, "Aulaen",
                LocalDateTime.now().plusDays(2), null, null);

        EventDTO result = dao.create(input);

        assertNotNull(result.id());
        assertNull(result.createdBy());
    }

    @Test
    @DisplayName("create med ukendt bruger giver 404, og intet bliver gemt")
    void createWithUnknownUser() {
        EventDTO input = new EventDTO(null, "Spøgelsesevent", null, null,
                LocalDateTime.now().plusDays(2), null, "findesikke");

        ApiException e = assertThrows(ApiException.class, () -> dao.create(input));

        assertEquals(404, e.getStatusCode());
        assertEquals(4, dao.getAll().size());
    }

    @Test
    @DisplayName("getById finder et eksisterende event")
    void getById() {
        EventDTO result = dao.getById(fredagsbar.getId());

        assertEquals("Fredagsbar", result.title());
        assertEquals("Kantinen", result.location());
        assertEquals(ADMIN, result.createdBy());
    }

    @Test
    @DisplayName("getById med ukendt id giver 404")
    void getByIdNotFound() {
        ApiException e = assertThrows(ApiException.class, () -> dao.getById(999_999L));
        assertEquals(404, e.getStatusCode());
    }

    @Test
    @DisplayName("getAll returnerer alle events sorteret efter starttidspunkt")
    void getAll() {
        List<String> titles = dao.getAll().stream().map(EventDTO::title).toList();

        assertEquals(List.of("Gammelt event", "Fredagsbar", "Hackathon", "Julefrokost"), titles);
    }

    @Test
    @DisplayName("getUpcoming(14) tager kun events de næste 14 dage – ikke fortid og ikke længere frem")
    void getUpcomingTwoWeeks() {
        List<String> titles = dao.getUpcoming(14).stream().map(EventDTO::title).toList();

        assertEquals(List.of("Fredagsbar", "Hackathon"), titles);
    }

    @Test
    @DisplayName("getUpcoming(30) tager også julefrokosten med")
    void getUpcomingMonth() {
        List<String> titles = dao.getUpcoming(30).stream().map(EventDTO::title).toList();

        assertEquals(List.of("Fredagsbar", "Hackathon", "Julefrokost"), titles);
    }

    @Test
    @DisplayName("getUpcoming(0) giver 400")
    void getUpcomingInvalidDays() {
        ApiException e = assertThrows(ApiException.class, () -> dao.getUpcoming(0));
        assertEquals(400, e.getStatusCode());
    }

    @Test
    @DisplayName("update ændrer kun de felter der er sendt med")
    void updatePartial() {
        EventDTO changes = new EventDTO(null, "Fredagsbar – Halloween", null, null, null, null, null);

        EventDTO result = dao.update(fredagsbar.getId(), changes);

        assertEquals("Fredagsbar – Halloween", result.title());
        assertEquals("Kantinen", result.location());
        assertEquals("Øl og musik", result.description());
    }

    @Test
    @DisplayName("update med ukendt id giver 404")
    void updateNotFound() {
        EventDTO changes = new EventDTO(null, "Ny titel", null, null, null, null, null);

        ApiException e = assertThrows(ApiException.class, () -> dao.update(999_999L, changes));
        assertEquals(404, e.getStatusCode());
    }

    @Test
    @DisplayName("delete fjerner eventet")
    void delete() {
        dao.delete(julefrokost.getId());

        assertEquals(3, dao.getAll().size());
        assertThrows(ApiException.class, () -> dao.getById(julefrokost.getId()));
    }

    @Test
    @DisplayName("delete med ukendt id giver 404")
    void deleteNotFound() {
        ApiException e = assertThrows(ApiException.class, () -> dao.delete(999_999L));
        assertEquals(404, e.getStatusCode());
    }
}
