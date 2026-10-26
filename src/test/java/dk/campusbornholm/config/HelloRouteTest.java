package dk.campusbornholm.config;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class HelloRouteTest {
    private static final int PORT = 7777;

    @BeforeAll
    static void setUp() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker kører ikke – testen springes over");
        ApplicationConfig.startServer(PORT, HibernateTestConfig.getEntityManagerFactory());
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = PORT;
        RestAssured.basePath = "/api";
    }

    @AfterAll
    static void tearDown() {
        ApplicationConfig.stopServer();
    }

    @Test
    @DisplayName("/hello svarer 200 med en besked")
    void helloReturnsMessage() {
        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body("msg", equalTo("hello fra Campus Bornholm"));
    }

    @Test
    @DisplayName("/auth/healthcheck svarer 200 uden token")
    void healthcheck() {
        given()
                .when().get("/auth/healthcheck")
                .then()
                .statusCode(200)
                .body("msg", equalTo("API is up and running"));
    }

    @Test
    @DisplayName("En ukendt route giver 404")
    void unknownRouteReturns404() {
        given()
                .when().get("/findes-ikke")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("CORS: et request med Origin får Access-Control-Allow-Origin tilbage")
    void corsHeader() {
        given()
                .header("Origin", "http://localhost:5173")
                .when().get("/hello")
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", notNullValue());
    }
}
