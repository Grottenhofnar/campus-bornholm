package dk.campusbornholm.testutils;

import dk.campusbornholm.entities.Role;
import dk.campusbornholm.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public final class UserPopulator {

    public static final String USER = "testuser";
    public static final String ADMIN = "testadmin";
    public static final String PASSWORD = "test12345";

    private UserPopulator() {
    }

    public static Map<String, User> populate(EntityManagerFactory emf) {
        TestUtils.truncateAll(emf);

        Map<String, User> users = new LinkedHashMap<>();
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            Role userRole = new Role("USER");
            Role adminRole = new Role("ADMIN");
            em.persist(userRole);
            em.persist(adminRole);

            User user = new User(USER, PASSWORD);
            user.addRole(userRole);
            em.persist(user);

            User admin = new User(ADMIN, PASSWORD);
            admin.addRole(userRole);
            admin.addRole(adminRole);
            em.persist(admin);

            em.getTransaction().commit();

            users.put(USER, user);
            users.put(ADMIN, admin);
        }
        return users;
    }
}
