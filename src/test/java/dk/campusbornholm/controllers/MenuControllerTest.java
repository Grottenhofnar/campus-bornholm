package dk.campusbornholm.controllers;

import dk.campusbornholm.config.AppClock;
import dk.campusbornholm.config.ApplicationConfig;
import dk.campusbornholm.config.HibernateTestConfig;
import dk.campusbornholm.testutils.DishPopulator;
import dk.campusbornholm.testutils.TestUtils;
import io.restassured.RestAssured;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.DockerClientFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MenuControllerTest {

    private static final int PORT = 7777;

    private EntityManagerFactory emf;
    private LocalDate today;
    private LocalDate monday;

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
        DishPopulator.populate(emf);
        today = AppClock.today();
        monday = today.with(DayOfWeek.MONDAY);
    }

    @AfterAll
    void tearDownAll() {
        ApplicationConfig.stopServer();
        if (emf != null) {
            TestUtils.truncateAll(emf);
        }
    }

    @Test
    @DisplayName("/menu/today returnerer kun dagens ret")
    void today() {
        given()
                .when()
                .get("/menu/today")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].date", equalTo(today.toString()))
                .body("[0].name", equalTo("Ret " + today))
                .body("[0].price", equalTo(40.0f));
    }

    @Test
    @DisplayName("/menu/week returnerer ugenummer og mandag–fredag i den aktuelle uge")
    void week() {
        int weekNumber = today.get(WeekFields.ISO.weekOfWeekBasedYear());

        given()
                .when()
                .get("/menu/week")
                .then()
                .statusCode(200)
                .body("weekNumber", equalTo(weekNumber))
                .body("from", equalTo(monday.toString()))
                .body("to", equalTo(monday.plusDays(4).toString()));
    }

    @Test
    @DisplayName("/menu/week tager kun hverdage med – ikke weekend, forrige eller næste uge")
    void weekOnlyWeekdays() {
        given()
                .when()
                .get("/menu/week")
                .then()
                .statusCode(200)
                .body("dishes", hasSize(5))
                .body("dishes.date", contains(
                        monday.toString(),
                        monday.plusDays(1).toString(),
                        monday.plusDays(2).toString(),
                        monday.plusDays(3).toString(),
                        monday.plusDays(4).toString()));
    }

    @Test
    @DisplayName("Menuen kræver ikke login")
    void noTokenNeeded() {
        given().when().get("/menu/today").then().statusCode(200);
        given().when().get("/menu/week").then().statusCode(200);
    }

    @Test
    @DisplayName("Uden retter i databasen giver /menu/today en tom liste")
    void emptyMenu() {
        TestUtils.truncateAll(emf);

        given()
                .when()
                .get("/menu/today")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }
}
