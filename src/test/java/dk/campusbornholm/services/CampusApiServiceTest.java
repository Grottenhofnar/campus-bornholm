package dk.campusbornholm.services;

import dk.campusbornholm.dtos.DishDTO;
import dk.campusbornholm.dtos.EventDTO;
import dk.campusbornholm.dtos.external.CampusActivityDTO;
import dk.campusbornholm.dtos.external.CampusMenuDTO;
import dk.campusbornholm.exceptions.ApiException;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static io.javalin.apibuilder.ApiBuilder.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CampusApiServiceTest {

    private static final int PORT = 7778;
    private static final String FAKE_URL = "http://localhost:" + PORT;

    private static Javalin fakeApi;
    private static CampusApiService service;

    @BeforeAll
    static void setUp() throws IOException {
        String menuJson = resource("/campus/menu.json");
        String activitiesJson = resource("/campus/activities.json");

        fakeApi = Javalin.create(config -> config.router.apiBuilder(() -> {
            get("/menu", ctx -> ctx.contentType("application/json").result(menuJson));
            get("/activities", ctx -> ctx.contentType("application/json").result(activitiesJson));
            get("/broken/menu", ctx -> ctx.status(500).result("fejl"));
            get("/invalid/menu", ctx -> ctx.contentType("application/json").result("{ikke json"));
        })).start(PORT);

        service = new CampusApiService(FAKE_URL);
    }

    @AfterAll
    static void tearDown() {
        if (fakeApi != null) {
            fakeApi.stop();
        }
    }

    @Test
    @DisplayName("fetchMenu læser uge, år og alle retter")
    void fetchMenu() {
        CampusMenuDTO menu = service.fetchMenu();

        assertEquals(44, menu.weekNumber());
        assertEquals(2026, menu.year());
        assertEquals(4, menu.items().size());
        assertEquals(LocalDate.of(2026, 10, 26), menu.items().get(0).date());
    }

    @Test
    @DisplayName("fetchActivities læser alle aktiviteter med dato og tid")
    void fetchActivities() {
        List<CampusActivityDTO> activities = service.fetchActivities();

        assertEquals(3, activities.size());
        assertEquals(LocalDateTime.of(2026, 10, 26, 8, 0), activities.get(0).startDateTime());
    }

    @Test
    @DisplayName("fetchDishes mapper til DishDTO og springer retter uden titel over")
    void fetchDishes() {
        List<DishDTO> dishes = service.fetchDishes();

        assertEquals(3, dishes.size());

        DishDTO first = dishes.get(0);
        assertNull(first.id());
        assertEquals("Cajun pasta", first.name());
        assertEquals("med kylling", first.description());
        assertEquals(40.0, first.price());
        assertEquals(LocalDate.of(2026, 10, 26), first.date());
        assertEquals(FAKE_URL + "/menu", first.sourceUrl());
    }

    @Test
    @DisplayName("Titler trimmes, og tom beskrivelse bliver null")
    void dishCleanup() {
        DishDTO lasagne = service.fetchDishes().get(1);

        assertEquals("Lasagne", lasagne.name());
        assertNull(lasagne.description());
        assertEquals(45.5, lasagne.price());
    }

    @Test
    @DisplayName("fetchEvents mapper til EventDTO uden opretter og springer aktiviteter uden starttid over")
    void fetchEvents() {
        List<EventDTO> events = service.fetchEvents();

        assertEquals(2, events.size());

        EventDTO kran = events.get(0);
        assertEquals("AMU Kursus Kran", kran.title());
        assertEquals("Ved Lunden 3", kran.location());
        assertEquals(LocalDateTime.of(2026, 11, 6, 16, 0), kran.endTime());
        assertNull(kran.createdBy());
        assertNull(events.get(1).location());
    }

    @Test
    @DisplayName("Fejlstatus fra API'et giver ApiException med 502")
    void serverError() {
        CampusApiService broken = new CampusApiService(FAKE_URL + "/broken");

        ApiException e = assertThrows(ApiException.class, broken::fetchMenu);
        assertEquals(502, e.getStatusCode());
    }

    @Test
    @DisplayName("Ugyldig JSON giver ApiException med 502")
    void invalidJson() {
        CampusApiService invalid = new CampusApiService(FAKE_URL + "/invalid");

        ApiException e = assertThrows(ApiException.class, invalid::fetchMenu);
        assertEquals(502, e.getStatusCode());
    }

    @Test
    @DisplayName("API'et kan ikke nås giver ApiException med 502")
    void unreachable() {
        CampusApiService offline = new CampusApiService("http://localhost:1");

        ApiException e = assertThrows(ApiException.class, offline::fetchMenu);
        assertEquals(502, e.getStatusCode());
    }

    @Test
    @DisplayName("toDishes og toEvents håndterer null uden at fejle")
    void nullSafety() {
        assertEquals(List.of(), CampusApiService.toDishes(null, FAKE_URL));
        assertEquals(List.of(), CampusApiService.toEvents(null));
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = CampusApiServiceTest.class.getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
