package dk.campusbornholm.testutils;

import dk.campusbornholm.config.AppClock;
import dk.campusbornholm.entities.Event;
import dk.campusbornholm.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EventPopulator {

    public static final String ADMIN = "testadmin";

    private EventPopulator() {
    }

    public static Map<String, Event> populate(EntityManagerFactory emf) {
        TestUtils.truncateAll(emf);

        LocalDateTime now = AppClock.now();
        Map<String, Event> events = new LinkedHashMap<>();

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            User admin = new User(ADMIN, "test12345");
            em.persist(admin);

            events.put("fredagsbar", new Event("Fredagsbar", "Øl og musik", "Kantinen",
                    now.plusDays(1), now.plusDays(1).plusHours(4), admin));
            events.put("hackathon", new Event("Hackathon", "24 timers kodning", "Lokale 2.10",
                    now.plusDays(13), now.plusDays(14), admin));
            events.put("julefrokost", new Event("Julefrokost", "Årets fest", "Kantinen",
                    now.plusDays(20), now.plusDays(20).plusHours(6), admin));
            events.put("gammelt", new Event("Gammelt event", "Er allerede sket", "Aulaen",
                    now.minusDays(1), now.minusDays(1).plusHours(2), null));

            events.values().forEach(em::persist);
            em.getTransaction().commit();
        }
        return events;
    }
}
