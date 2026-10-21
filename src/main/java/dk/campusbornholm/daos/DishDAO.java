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

public class DishDAO implements IDAO<DishDTO, Long> {
    private final EntityManagerFactory emf;

    public DishDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public DishDTO create(DishDTO dto) {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                em.getTransaction().begin();
                Dish dish = toEntity(dto);
                em.persist(dish);
                em.getTransaction().commit();
                return new DishDTO(dish);
            } catch (RuntimeException e) {
                DAOUtils.rollbackIfActive(em);
                throw e;
            }
        }
    }

    @Override
    public DishDTO getById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return new DishDTO(findOrThrow(em, id));
        }
    }

    @Override
    public List<DishDTO> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT d FROM Dish d ORDER BY d.date", Dish.class)
                    .getResultStream()
                    .map(DishDTO::new)
                    .toList();
        }
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

                List<Dish> dishes = dtos.stream().map(this::toEntity).toList();
                dishes.forEach(em::persist);

                em.getTransaction().commit();
                return dishes.stream().map(DishDTO::new).toList();
            } catch (RuntimeException e) {
                DAOUtils.rollbackIfActive(em);
                throw e;
            }
        }
    }

    @Override
    public DishDTO update(Long id, DishDTO dto) {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                em.getTransaction().begin();
                Dish dish = findOrThrow(em, id);
                if (dto.name() != null) dish.setName(dto.name());
                if (dto.description() != null) dish.setDescription(dto.description());
                if (dto.price() != null) dish.setPrice(dto.price());
                if (dto.date() != null) dish.setDate(dto.date());
                if (dto.sourceUrl() != null) dish.setSourceUrl(dto.sourceUrl());
                em.getTransaction().commit();
                return new DishDTO(dish);
            } catch (RuntimeException e) {
                DAOUtils.rollbackIfActive(em);
                throw e;
            }
        }
    }

    @Override
    public void delete(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                em.getTransaction().begin();
                em.remove(findOrThrow(em, id));
                em.getTransaction().commit();
            } catch (RuntimeException e) {
                DAOUtils.rollbackIfActive(em);
                throw e;
            }
        }
    }

    private Dish toEntity(DishDTO dto) {
        return new Dish(dto.name(), dto.description(), dto.price(), dto.date(), dto.sourceUrl());
    }

    private Dish findOrThrow(EntityManager em, Long id) {
        Dish dish = em.find(Dish.class, id);
        if (dish == null) {
            throw new ApiException(404, "Ret med id " + id + " findes ikke");
        }
        return dish;
    }
}
