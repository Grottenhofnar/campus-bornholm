package dk.campusbornholm.controllers;

import dk.campusbornholm.config.ApplicationConfig;
import dk.campusbornholm.config.HibernateTestConfig;
import dk.campusbornholm.entities.News;
import dk.campusbornholm.testutils.NewsPopulator;
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

import java.util.Map;

import static dk.campusbornholm.testutils.TestUtils.bearer;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NewsControllerTest {

    private static final int PORT = 7777;

    private EntityManagerFactory emf;
    private Map<String, News> seeded;
    private String userToken;
    private String adminToken;

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
        seeded = NewsPopulator.populate(emf);
        userToken = TestUtils.login(UserPopulator.USER, UserPopulator.PASSWORD);
        adminToken = TestUtils.login(UserPopulator.ADMIN, UserPopulator.PASSWORD);
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

    @Test
    @DisplayName("GET /news returnerer alle nyheder, nyeste først, uden login")
    void getAll() {
        given()
                .when()
                .get("/news")
                .then()
                .statusCode(200)
                .body("$", hasSize(3))
                .body("title", contains("Fredagsbar", "Ny kantinemenu", "Studiestart"));
    }

    @Test
    @DisplayName("GET /news?limit=2 returnerer de 2 nyeste")
    void getLatest() {
        given()
                .queryParam("limit", 2)
                .when()
                .get("/news")
                .then()
                .statusCode(200)
                .body("title", contains("Fredagsbar", "Ny kantinemenu"));
    }

    @Test
    @DisplayName("GET /news med ugyldig limit giver 400")
    void getLatestInvalid() {
        given().queryParam("limit", 0).when().get("/news").then().statusCode(400);
        given().queryParam("limit", "abc").when().get("/news").then().statusCode(400);
    }

    @Test
    @DisplayName("GET /news/{id} returnerer nyheden med forfatter")
    void getById() {
        given()
                .when()
                .get("/news/" + id("midt"))
                .then()
                .statusCode(200)
                .body("id", equalTo(id("midt").intValue()))
                .body("title", equalTo("Ny kantinemenu"))
                .body("authorName", equalTo(UserPopulator.ADMIN))
                .body("createdAt", notNullValue());
    }

    @Test
    @DisplayName("GET /news/{id} med ukendt id giver 404")
    void getByIdNotFound() {
        given()
                .when()
                .get("/news/999999")
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("msg", containsString("999999"));
    }

    @Test
    @DisplayName("POST som admin giver 201, og forfatteren kommer fra tokenet")
    void createAsAdmin() {
        given()
                .header("Authorization", bearer(adminToken))
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Ny nyhed", "content", "Indhold", "authorName", "nogenandre"))
                .when()
                .post("/news")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo("Ny nyhed"))
                .body("authorName", equalTo(UserPopulator.ADMIN));

        given().when().get("/news").then().body("$", hasSize(4));
    }

    @Test
    @DisplayName("POST uden token giver 401")
    void createWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Ny nyhed", "content", "Indhold"))
                .when()
                .post("/news")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("POST som almindelig bruger giver 403")
    void createAsUser() {
        given()
                .header("Authorization", bearer(userToken))
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Ny nyhed", "content", "Indhold"))
                .when()
                .post("/news")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("POST uden indhold giver 400")
    void createWithoutContent() {
        given()
                .header("Authorization", bearer(adminToken))
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Kun en titel"))
                .when()
                .post("/news")
                .then()
                .statusCode(400)
                .body("msg", containsString("content"));
    }

    @Test
    @DisplayName("POST med en titel på over 255 tegn giver 400")
    void createTitleTooLong() {
        given()
                .header("Authorization", bearer(adminToken))
                .contentType(ContentType.JSON)
                .body(Map.of("title", "x".repeat(256), "content", "Indhold"))
                .when()
                .post("/news")
                .then()
                .statusCode(400)
                .body("msg", containsString("255"));
    }

    @Test
    @DisplayName("PATCH ændrer kun titlen og lader indholdet være")
    void patchTitle() {
        given()
                .header("Authorization", bearer(adminToken))
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Fredagsbar – flyttet"))
                .when()
                .patch("/news/" + id("nyest"))
                .then()
                .statusCode(200)
                .body("title", equalTo("Fredagsbar – flyttet"))
                .body("content", equalTo("Fredagsbar i kantinen kl. 16"));
    }

    @Test
    @DisplayName("PATCH uden felter giver 400")
    void patchEmpty() {
        given()
                .header("Authorization", bearer(adminToken))
                .contentType(ContentType.JSON)
                .body(Map.of())
                .when()
                .patch("/news/" + id("nyest"))
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("PATCH som almindelig bruger giver 403")
    void patchAsUser() {
        given()
                .header("Authorization", bearer(userToken))
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Hacket"))
                .when()
                .patch("/news/" + id("nyest"))
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("DELETE som admin giver 204, og nyheden er væk bagefter")
    void deleteAsAdmin() {
        given()
                .header("Authorization", bearer(adminToken))
                .when()
                .delete("/news/" + id("aeldst"))
                .then()
                .statusCode(204);

        given().when().get("/news/" + id("aeldst")).then().statusCode(404);
    }

    @Test
    @DisplayName("DELETE med ukendt id giver 404")
    void deleteNotFound() {
        given()
                .header("Authorization", bearer(adminToken))
                .when()
                .delete("/news/999999")
                .then()
                .statusCode(404);
    }
}
