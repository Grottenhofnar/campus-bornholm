package dk.campusbornholm.testutils;

import io.restassured.http.ContentType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.Map;

import static io.restassured.RestAssured.given;

public final class TestUtils {
    private TestUtils() {
    }

    public static void truncateAll(EntityManagerFactory emf) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("""
                    TRUNCATE TABLE events, dishes, user_roles, users, roles
                    RESTART IDENTITY CASCADE
                    """).executeUpdate();
            em.getTransaction().commit();
        }
    }

    public static String login(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }
}
