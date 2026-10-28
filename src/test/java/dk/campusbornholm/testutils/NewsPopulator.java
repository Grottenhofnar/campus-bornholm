package dk.campusbornholm.testutils;

import dk.campusbornholm.config.AppClock;
import dk.campusbornholm.entities.News;
import dk.campusbornholm.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NewsPopulator {

    private NewsPopulator() {
    }

    public static Map<String, News> populate(EntityManagerFactory emf) {
        Map<String, User> users = UserPopulator.populate(emf);
        LocalDateTime now = AppClock.now();
        Map<String, News> news = new LinkedHashMap<>();

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            User admin = em.find(User.class, users.get(UserPopulator.ADMIN).getId());

            news.put("aeldst", new News("Studiestart", "Velkommen til nye studerende", admin));
            news.put("midt", new News("Ny kantinemenu", "Kantinen har fået ny menu", admin));
            news.put("nyest", new News("Fredagsbar", "Fredagsbar i kantinen kl. 16", admin));
            news.values().forEach(em::persist);

            news.get("aeldst").setCreatedAt(now.minusDays(3));
            news.get("midt").setCreatedAt(now.minusDays(2));
            news.get("nyest").setCreatedAt(now.minusDays(1));

            em.getTransaction().commit();
        }
        return news;
    }
}
