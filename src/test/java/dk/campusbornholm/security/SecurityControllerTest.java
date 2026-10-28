package dk.campusbornholm.security;

import dk.campusbornholm.config.ApplicationConfig;
import dk.campusbornholm.config.HibernateTestConfig;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityControllerTest {

    private static final int PORT = 7777;

    private EntityManagerFactory emf;
    private String userToken;

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
        UserPopulator.populate(emf);
        userToken = TestUtils.login(UserPopulator.USER, UserPopulator.PASSWORD);
    }

    @AfterAll
    void tearDownAll() {
        ApplicationConfig.stopServer();
        if (emf != null) {
            TestUtils.truncateAll(emf);
        }
    }

    @Test
    @DisplayName("Register opretter en bruger med rollen USER og returnerer et token")
    void registerCreatesUser() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "nybruger", "password", "hemmelig123"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201)
                .body("token", notNullValue())
                .body("username", equalTo("nybruger"))
                .body("roles", hasSize(1))
                .body("roles", hasItem("USER"))
                .body("password", nullValue());
    }

    @Test
    @DisplayName("Tokenet fra register kan bruges med det samme")
    void registerTokenWorks() {
        String token = given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "nybruger", "password", "hemmelig123"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201)
                .extract()
                .path("token");

        given()
                .header("Authorization", bearer(token))
                .when()
                .get("/auth/me")
                .then()
                .statusCode(200)
                .body("username", equalTo("nybruger"));
    }

    @Test
    @DisplayName("Register med et brugernavn der er taget giver 409")
    void registerTakenUsername() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", UserPopulator.USER, "password", "hemmelig123"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(409)
                .body("status", equalTo(409))
                .body("msg", containsString("optaget"));
    }

    @Test
    @DisplayName("Register med ugyldigt brugernavn og for kort password giver 400")
    void registerInvalid() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "e!", "password", "kort"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(400)
                .body("msg", containsString("username"))
                .body("msg", containsString("password"));
    }

    @Test
    @DisplayName("Register med ugyldig JSON giver 400")
    void registerInvalidJson() {
        given()
                .contentType(ContentType.JSON)
                .body("{ \"username\": ")
                .when()
                .post("/auth/register")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Login som bruger giver 200 med token og rollen USER")
    void loginUser() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", UserPopulator.USER, "password", UserPopulator.PASSWORD))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("username", equalTo(UserPopulator.USER))
                .body("roles", hasItem("USER"));
    }

    @Test
    @DisplayName("Login som admin giver både rollen USER og ADMIN")
    void loginAdmin() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", UserPopulator.ADMIN, "password", UserPopulator.PASSWORD))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("roles", hasItems("USER", "ADMIN"));
    }

    @Test
    @DisplayName("Login med forkert password giver 401")
    void loginWrongPassword() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", UserPopulator.USER, "password", "forkert123"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401)
                .body("msg", equalTo("Forkert brugernavn eller password"));
    }

    @Test
    @DisplayName("Login med ukendt bruger giver samme 401-besked som forkert password")
    void loginUnknownUser() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "findesikke", "password", "hemmelig123"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401)
                .body("msg", equalTo("Forkert brugernavn eller password"));
    }

    @Test
    @DisplayName("/auth/me med gyldigt token returnerer brugeren")
    void meWithToken() {
        given()
                .header("Authorization", bearer(userToken))
                .when()
                .get("/auth/me")
                .then()
                .statusCode(200)
                .body("username", equalTo(UserPopulator.USER))
                .body("roles", hasItem("USER"));
    }

    @Test
    @DisplayName("/auth/me uden token giver 401")
    void meWithoutToken() {
        given()
                .when()
                .get("/auth/me")
                .then()
                .statusCode(401)
                .body("msg", containsString("Authorization"));
    }

    @Test
    @DisplayName("/auth/me med et ødelagt token giver 401")
    void meWithBrokenToken() {
        given()
                .header("Authorization", bearer("abc.def.ghi"))
                .when()
                .get("/auth/me")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("/auth/me uden 'Bearer ' foran tokenet giver 401")
    void meWithMalformedHeader() {
        given()
                .header("Authorization", userToken)
                .when()
                .get("/auth/me")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("En USER på et ADMIN-endpoint giver 403")
    void userOnAdminEndpoint() {
        given()
                .header("Authorization", bearer(userToken))
                .when()
                .delete("/news/1")
                .then()
                .statusCode(403)
                .body("msg", containsString("ADMIN"));
    }
}
