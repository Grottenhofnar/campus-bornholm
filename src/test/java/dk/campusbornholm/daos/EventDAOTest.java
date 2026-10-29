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

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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

    @Test
    @DisplayName("create gemmer et event og returnerer det med id og opretter")
    void create() {
        EventDTO input = new EventDTO(null, "Brætspilsaften", null, "Loungen",
                AppClock.now().plusDays(5), null, EventPopulator.ADMIN);

        EventDTO result = dao.create(input);

        assertThat(result.id(), is(notNullValue()));
        assertThat(result.createdBy(), is(EventPopulator.ADMIN));
        assertThat(dao.getAll(), hasSize(5));
    }

    @Test
    @DisplayName("getById med ukendt id giver 404")
    void getByIdNotFound() {
        ApiException e = assertThrows(ApiException.class, () -> dao.getById(999_999L));
        assertThat(e.getStatusCode(), is(404));
    }

    @Test
    @DisplayName("getUpcoming(14) tager kun events de næste 14 dage – ikke fortid og ikke længere frem")
    void getUpcoming() {
        List<String> titles = dao.getUpcoming(14).stream().map(EventDTO::title).toList();

        assertThat(titles, contains("Fredagsbar", "Hackathon"));
    }

    @Test
    @DisplayName("replaceImported erstatter events uden opretter og rører ikke admins events")
    void replaceImported() {
        dao.replaceImported(List.of(
                new EventDTO(null, "AMU Kursus Kran", null, "Ved Lunden 3", AppClock.now().plusDays(3), null, null)));

        List<String> titles = dao.getAll().stream().map(EventDTO::title).toList();

        assertThat(titles, containsInAnyOrder("Fredagsbar", "Hackathon", "Julefrokost", "AMU Kursus Kran"));
    }
}
