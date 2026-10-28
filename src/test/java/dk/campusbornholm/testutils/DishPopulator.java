package dk.campusbornholm.testutils;

import dk.campusbornholm.config.AppClock;
import dk.campusbornholm.entities.Dish;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DishPopulator {

    private DishPopulator() {
    }

    public static Map<LocalDate, Dish> populate(EntityManagerFactory emf) {
        TestUtils.truncateAll(emf);

        LocalDate monday = AppClock.today().with(DayOfWeek.MONDAY);
        Map<LocalDate, Dish> dishes = new LinkedHashMap<>();

        for (int i = -1; i <= 7; i++) {
            LocalDate date = monday.plusDays(i);
            dishes.put(date, new Dish("Ret " + date, "Beskrivelse " + date, 40.0, date, "test"));
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            dishes.values().forEach(em::persist);
            em.getTransaction().commit();
        }
        return dishes;
    }
}
