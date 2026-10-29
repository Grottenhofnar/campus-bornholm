package dk.campusbornholm.daos;

import dk.campusbornholm.dtos.DishDTO;
import dk.campusbornholm.entities.Dish;
import dk.campusbornholm.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DishDAO {

    private final EntityManagerFactory emf;

    public DishDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public List<DishDTO> getByDate(LocalDate date) {
        return getByDateRange(date, date);
    }

    public List<DishDTO> getByDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new ApiException(400, "Startdato skal være før slutdato");
        }
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT d FROM Dish d WHERE d.date BETWEEN :from AND :to ORDER BY d.date", Dish.class)
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .getResultStream()
                    .map(DishDTO::new)
                    .toList();
        }
    }

    public List<DishDTO> saveAll(List<DishDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }
        Set<LocalDate> dates = dtos.stream().map(DishDTO::date).collect(Collectors.toSet());

        try (EntityManager em = emf.createEntityManager()) {
            try {
                em.getTransaction().begin();
                em.createQuery("DELETE FROM Dish d WHERE d.date IN :dates")
                        .setParameter("dates", dates)
                        .executeUpdate();

                List<Dish> dishes = dtos.stream()
                        .map(dto -> new Dish(dto.name(), dto.description(), dto.price(), dto.date(), dto.sourceUrl()))
                        .toList();
                dishes.forEach(em::persist);

                em.getTransaction().commit();
                return dishes.stream().map(DishDTO::new).toList();
            } catch (RuntimeException e) {
                DAOUtils.rollbackIfActive(em);
                throw e;
            }
        }
    }
}
