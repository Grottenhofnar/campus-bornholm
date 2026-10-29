package dk.campusbornholm.controllers;

import dk.campusbornholm.config.AppClock;
import dk.campusbornholm.config.ApplicationConfig;
import dk.campusbornholm.config.HibernateTestConfig;
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

import java.time.temporal.ChronoUnit;
import java.util.Map;

import static dk.campusbornholm.testutils.TestUtils.bearer;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventControllerTest {

    private static final int PORT = 7777;

    private EntityManagerFactory emf;
    private String userToken;
    private String adminToken;
    private Map<String, Object> newEvent;

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
        EventPopulator.populate(emf);
        userToken = TestUtils.login(UserPopulator.USER, UserPopulator.PASSWORD);
        adminToken = TestUtils.login(UserPopulator.ADMIN, UserPopulator.PASSWORD);
        newEvent = Map.of(
                "title", "Brætspilsaften",
                "startTime", AppClock.now().plusDays(5).truncatedTo(ChronoUnit.HOURS).toString());
    }

    @AfterAll
    void tearDownAll() {
        ApplicationConfig.stopServer();
        if (emf != null) {
            TestUtils.truncateAll(emf);
        }
    }

    @Test
    @DisplayName("GET /events er åben og viser de næste 14 dage")
    void getUpcoming() {
        given()
                .when()
                .get("/events")
                .then()
                .statusCode(200)
                .body("title", contains("Fredagsbar", "Hackathon"));
    }

    @Test
    @DisplayName("POST som admin giver 201, og opretteren kommer fra tokenet")
    void createAsAdmin() {
        given()
                .header("Authorization", bearer(adminToken))
                .contentType(ContentType.JSON)
                .body(newEvent)
                .when()
                .post("/events")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo("Brætspilsaften"))
                .body("createdBy", equalTo(UserPopulator.ADMIN));
    }

    @Test
    @DisplayName("POST uden token giver 401")
    void createWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body(newEvent)
                .when()
                .post("/events")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("POST som almindelig bruger giver 403")
    void createAsUser() {
        given()
                .header("Authorization", bearer(userToken))
                .contentType(ContentType.JSON)
                .body(newEvent)
                .when()
                .post("/events")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("POST uden titel giver 400")
    void createInvalid() {
        given()
                .header("Authorization", bearer(adminToken))
                .contentType(ContentType.JSON)
                .body(Map.of("startTime", newEvent.get("startTime")))
                .when()
                .post("/events")
                .then()
                .statusCode(400)
                .body("msg", containsString("title"));
    }
}
