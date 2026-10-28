package dk.campusbornholm.controllers;

import dk.campusbornholm.config.AppClock;
import dk.campusbornholm.config.ApplicationConfig;
import dk.campusbornholm.config.HibernateTestConfig;
import dk.campusbornholm.entities.Event;
import dk.campusbornholm.testutils.EventPopulator;
import dk.campusbornholm.testutils.TestUtils;
import dk.campusbornholm.testutils.UserPopulator;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.DockerClientFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static dk.campusbornholm.testutils.TestUtils.bearer;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventControllerTest {

    private static final int PORT = 7777;

    private EntityManagerFactory emf;
    private Map<String, Event> seeded;
    private String userToken;
    private String adminToken;
    private LocalDateTime start;

    @BeforeAll
    void setUpAll() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker kører ikke – testen springes over");
        emf = HibernateTestConfig.getEntityManagerFactory();
        ApplicationConfig.startServer(PORT, emf);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = PORT;
        RestAssured.basePath = "/api";
    }

    @BeforeEach
    void setUp() {
        seeded = EventPopulator.populate(emf);
        userToken = TestUtils.login(UserPopulator.USER, UserPopulator.PASSWORD);
        adminToken = TestUtils.login(UserPopulator.ADMIN, UserPopulator.PASSWORD);
        start = AppClock.now().plusDays(5).truncatedTo(ChronoUnit.HOURS);
    }

    @AfterAll
    void tearDownAll() {
        ApplicationConfig.stopServer();
        if (emf != null) {
            TestUtils.truncateAll(emf);
        }
    }

    private Long id(String key) {
        return seeded.get(key).getId();
    }

    private String iso(LocalDateTime time) {
        return time.toString();
    }

    @Test
    @DisplayName("GET /events uden days viser de næste 14 dage, tidligste først, uden login")
    void upcomingDefault() {
        given()
                .when()
                .get("/events")
                .then()
                .statusCode(200)
                .body("title", contains("Fredagsbar", "Hackathon"));
    }

    @Test
    @DisplayName("GET /events?days=30 tager også julefrokosten med – men aldrig fortidige events")
    void upcomingThirtyDays() {
        given()
                .queryParam("days", 30)
                .when()
                .get("/events")
                .then()
                .statusCode(200)
                .body("title", contains("Fredagsbar", "Hackathon", "Julefrokost"));
    }

    @Test
    @DisplayName("GET /events med ugyldig days giver 400")
    void upcomingInvalidDays() {
        given().queryParam("days", 0).when().get("/events").then().statusCode(400);
        given().queryParam("days", 366).when().get("/events").then().statusCode(400);
        given().queryParam("days", "abc").when().get("/events").then().statusCode(400);
    }

    @Test
    @DisplayName("GET /events/{id} returnerer eventet med opretter")
    void getById() {
        given()
                .when()
                .get("/events/" + id("hackathon"))
                .then()
                .statusCode(200)
                .body("id", equalTo(id("hackathon").intValue()))
                .body("title", equalTo("Hackathon"))
                .body("location", equalTo("Lokale 2.10"))
                .body("createdBy", equalTo(UserPopulator.ADMIN));
    }

    @Test
    @DisplayName("GET /events/{id} med ukendt id giver 404, og id der ikke er et tal giver 400")
    void getByIdErrors() {
        given().when().get("/events/999999").then().statusCode(404);
        given().when().get("/events/abc").then().statusCode(400);
    }

    @Test
    @DisplayName("POST som admin giver 201, og opretteren kommer fra tokenet")
    void createAsAdmin() {
        given()
                .header("Authorization", bearer(adminToken))
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "Brætspilsaften",
                        "location", "Loungen",
                        "startTime", iso(start),
                        "endTime", iso(start.plusHours(3)),
                        "createdBy", "nogenandre"))
                .when()
                .post("/events")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo("Brætspilsaften"))
                .body("startTime", startsWith(start.toLocalDate().toString()))
                .body("createdBy", equalTo(UserPopulator.ADMIN));
    }

    @Test
    @DisplayName("POST uden token giver 401, og som almindelig bruger 403")
    void createWithoutRights() {
        Map<String, Object> body = Map.of("title", "Brætspilsaften", "startTime", iso(start));

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/events")
                .then()
                .statusCode(401);

        given()
                .header("Authorization", bearer(userToken))
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/events")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("POST uden titel og starttidspunkt giver 400")
    void createMissingFields() {
        given()
                .header("Authorization", bearer(adminToken))
                .contentType(ContentType.JSON)
                .body(Map.of("location", "Loungen"))
                .when()
                .post("/events")
                .then()
                .statusCode(400)
                .body("msg", containsString("title"))
                .body("msg", containsString("startTime"));
    }

    @Test
    @DisplayName("POST med slut før start giver 400")
    void createEndBeforeStart() {
        given()
                .header("Authorization", bearer(adminToken))
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "Baglæns",
                        "startTime", iso(start),
                        "endTime", iso(start.minusHours(2))))
                .when()
                .post("/events")
                .then()
                .statusCode(400)
                .body("msg", containsString("endTime"));
    }

    @Test
    @DisplayName("PATCH ændrer kun stedet og lader titlen være")
    void patchLocation() {
        given()
                .header("Authorization", bearer(adminToken))
                .contentType(ContentType.JSON)
                .body(Map.of("location", "Aulaen"))
                .when()
                .patch("/events/" + id("fredagsbar"))
                .then()
                .statusCode(200)
                .body("location", equalTo("Aulaen"))
                .body("title", equalTo("Fredagsbar"));
    }

    @Test
    @DisplayName("PATCH med slut før start giver 400")
    void patchEndBeforeStart() {
        given()
                .header("Authorization", bearer(adminToken))
                .contentType(ContentType.JSON)
                .body(Map.of("startTime", iso(start), "endTime", iso(start.minusHours(1))))
                .when()
                .patch("/events/" + id("fredagsbar"))
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("PATCH som almindelig bruger giver 403")
    void patchAsUser() {
        given()
                .header("Authorization", bearer(userToken))
                .contentType(ContentType.JSON)
                .body(Map.of("location", "Hacket"))
                .when()
                .patch("/events/" + id("fredagsbar"))
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("DELETE som admin giver 204, og eventet er væk bagefter")
    void deleteAsAdmin() {
        given()
                .header("Authorization", bearer(adminToken))
                .when()
                .delete("/events/" + id("julefrokost"))
                .then()
                .statusCode(204);

        given().when().get("/events/" + id("julefrokost")).then().statusCode(404);
    }

    @Test
    @DisplayName("DELETE som almindelig bruger giver 403, og eventet findes stadig")
    void deleteAsUser() {
        given()
                .header("Authorization", bearer(userToken))
                .when()
                .delete("/events/" + id("julefrokost"))
                .then()
                .statusCode(403);

        given().when().get("/events/" + id("julefrokost")).then().statusCode(200);
    }
}
