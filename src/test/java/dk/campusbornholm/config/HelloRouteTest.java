package dk.campusbornholm.config;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class HelloRouteTest {
    private static final int PORT = 7777;

    @BeforeAll
    static void setUp() {
        ApplicationConfig.startServer(PORT);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = PORT;
        RestAssured.basePath = "/api";
    }

    @AfterAll
    static void tearDown() {
        ApplicationConfig.stopServer();
    }

    @Test
    void helloReturnsMessage() {
        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body("msg", equalTo("hello fra Campus Bornholm"));
    }

    @Test
    void unknownRouteReturns404() {
        given()
                .when().get("/findes-ikke")
                .then()
                .statusCode(404);
    }
}
