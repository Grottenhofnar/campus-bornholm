package dk.campusbornholm.daos;

import dk.campusbornholm.config.AppClock;
import dk.campusbornholm.config.HibernateTestConfig;
import dk.campusbornholm.dtos.EventDTO;
import dk.campusbornholm.entities.Event;
import dk.campusbornholm.exceptions.ApiException;
import dk.campusbornholm.testutils.EventPopulator;
import dk.campusbornholm.testutils.TestUtils;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.DockerClientFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventDAOTest {

    private EntityManagerFactory emf;
    private EventDAO dao;
    private Map<String, Event> seeded;

    @BeforeAll
    void setUpAll() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker kører ikke – testen springes over");
        emf = HibernateTestConfig.getEntityManagerFactory();
        dao = new EventDAO(emf);
    }

    @BeforeEach
    void setUp() {
        seeded = EventPopulator.populate(emf);
    }

    @AfterAll
    void tearDownAll() {
        if (emf != null) {
            TestUtils.truncateAll(emf);
        }
    }

    private List<String> titles(List<EventDTO> events) {
        return events.stream().map(EventDTO::title).toList();
    }

    @Test
    @DisplayName("create gemmer et event og returnerer det med id og opretter")
    void create() {
        LocalDateTime start = AppClock.now().plusDays(5);
        EventDTO input = new EventDTO(null, "Brætspilsaften", "Medbring spil", "Loungen",
                start, start.plusHours(3), EventPopulator.ADMIN);

        EventDTO result = dao.create(input);

        assertThat(result.id(), is(notNullValue()));
        assertThat(result.title(), is("Brætspilsaften"));
        assertThat(result.createdBy(), is(EventPopulator.ADMIN));
        assertThat(dao.getAll(), hasSize(5));
    }

    @Test
    @DisplayName("create uden opretter er tilladt")
    void createWithoutCreator() {
        EventDTO input = new EventDTO(null, "Åbent hus", null, "Aulaen",
                AppClock.now().plusDays(2), null, null);

        EventDTO result = dao.create(input);

        assertThat(result.id(), is(notNullValue()));
        assertThat(result.createdBy(), is(nullValue()));
    }

    @Test
    @DisplayName("create med ukendt bruger giver 404, og intet bliver gemt")
    void createWithUnknownUser() {
        EventDTO input = new EventDTO(null, "Spøgelsesevent", null, null,
                AppClock.now().plusDays(2), null, "findesikke");

        ApiException e = assertThrows(ApiException.class, () -> dao.create(input));

        assertThat(e.getStatusCode(), is(404));
        assertThat(dao.getAll(), hasSize(4));
    }

    @Test
    @DisplayName("getById finder et eksisterende event")
    void getById() {
        EventDTO result = dao.getById(seeded.get("fredagsbar").getId());

        assertThat(result.title(), is("Fredagsbar"));
        assertThat(result.location(), is("Kantinen"));
        assertThat(result.createdBy(), is(EventPopulator.ADMIN));
    }

    @Test
    @DisplayName("getById med ukendt id giver 404")
    void getByIdNotFound() {
        ApiException e = assertThrows(ApiException.class, () -> dao.getById(999_999L));
        assertThat(e.getStatusCode(), is(404));
    }

    @Test
    @DisplayName("getAll returnerer alle events sorteret efter starttidspunkt")
    void getAll() {
        assertThat(titles(dao.getAll()), contains("Gammelt event", "Fredagsbar", "Hackathon", "Julefrokost"));
    }

    @Test
    @DisplayName("getUpcoming(14) tager kun events de næste 14 dage – ikke fortid og ikke længere frem")
    void getUpcomingTwoWeeks() {
        assertThat(titles(dao.getUpcoming(14)), contains("Fredagsbar", "Hackathon"));
    }

    @Test
    @DisplayName("getUpcoming(30) tager også julefrokosten med")
    void getUpcomingMonth() {
        assertThat(titles(dao.getUpcoming(30)), contains("Fredagsbar", "Hackathon", "Julefrokost"));
    }

    @Test
    @DisplayName("getUpcoming(0) giver 400")
    void getUpcomingInvalidDays() {
        ApiException e = assertThrows(ApiException.class, () -> dao.getUpcoming(0));
        assertThat(e.getStatusCode(), is(400));
    }

    @Test
    @DisplayName("update ændrer kun de felter der er sendt med")
    void updatePartial() {
        EventDTO changes = new EventDTO(null, "Fredagsbar – Halloween", null, null, null, null, null);

        EventDTO result = dao.update(seeded.get("fredagsbar").getId(), changes);

        assertThat(result.title(), is("Fredagsbar – Halloween"));
        assertThat(result.location(), is("Kantinen"));
        assertThat(result.description(), is("Øl og musik"));
    }

    @Test
    @DisplayName("update med ukendt id giver 404")
    void updateNotFound() {
        EventDTO changes = new EventDTO(null, "Ny titel", null, null, null, null, null);

        ApiException e = assertThrows(ApiException.class, () -> dao.update(999_999L, changes));
        assertThat(e.getStatusCode(), is(404));
    }

    @Test
    @DisplayName("replaceImported erstatter events uden opretter og rører ikke admins events")
    void replaceImported() {
        LocalDateTime start = AppClock.now().plusDays(3);
        List<EventDTO> imported = List.of(
                new EventDTO(null, "AMU Kursus Kran", null, "Ved Lunden 3", start, start.plusHours(8), null),
                new EventDTO(null, "Studiestart", null, "Aulaen", start.plusDays(1), null, null));

        int saved = dao.replaceImported(imported);

        List<String> titles = titles(dao.getAll());
        assertThat(saved, is(2));
        assertThat(titles, containsInAnyOrder("Fredagsbar", "Hackathon", "Julefrokost", "AMU Kursus Kran", "Studiestart"));
        assertThat(titles, not(hasItem("Gammelt event")));
    }

    @Test
    @DisplayName("replaceImported to gange giver ingen dubletter")
    void replaceImportedTwice() {
        LocalDateTime start = AppClock.now().plusDays(3);
        List<EventDTO> imported = List.of(
                new EventDTO(null, "AMU Kursus Kran", null, "Ved Lunden 3", start, null, null));

        dao.replaceImported(imported);
        dao.replaceImported(imported);

        assertThat(dao.getAll(), hasSize(4));
    }

    @Test
    @DisplayName("delete fjerner eventet")
    void delete() {
        Long id = seeded.get("julefrokost").getId();

        dao.delete(id);

        assertThat(dao.getAll(), hasSize(3));
        assertThrows(ApiException.class, () -> dao.getById(id));
    }

    @Test
    @DisplayName("delete med ukendt id giver 404")
    void deleteNotFound() {
        ApiException e = assertThrows(ApiException.class, () -> dao.delete(999_999L));
        assertThat(e.getStatusCode(), is(404));
    }
}
