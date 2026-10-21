package dk.campusbornholm.daos;

import dk.campusbornholm.dtos.EventDTO;
import dk.campusbornholm.entities.Event;
import dk.campusbornholm.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class EventDAO implements IDAO<EventDTO, Long> {
    private final EntityManagerFactory emf;

    public EventDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public EventDTO create(EventDTO dto) {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                em.getTransaction().begin();
                Event event = new Event(
                        dto.title(),
                        dto.description(),
                        dto.location(),
                        dto.startTime(),
                        dto.endTime(),
                        dto.createdBy() == null ? null : DAOUtils.findUserOrThrow(em, dto.createdBy()));
                em.persist(event);
                em.getTransaction().commit();
                return new EventDTO(event);
            } catch (RuntimeException e) {
                DAOUtils.rollbackIfActive(em);
                throw e;
            }
        }
    }

    @Override
    public EventDTO getById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return new EventDTO(findOrThrow(em, id));
        }
    }

    @Override
    public List<EventDTO> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT e FROM Event e LEFT JOIN FETCH e.createdBy ORDER BY e.startTime", Event.class)
                    .getResultStream()
                    .map(EventDTO::new)
                    .toList();
        }
    }

    public List<EventDTO> getUpcoming(int days) {
        if (days < 1) {
            throw new ApiException(400, "Antal dage skal være mindst 1");
        }
        LocalDateTime now = LocalDateTime.now();
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                            SELECT e FROM Event e
                            LEFT JOIN FETCH e.createdBy
                            WHERE e.startTime BETWEEN :from AND :to
                            ORDER BY e.startTime
                            """, Event.class)
                    .setParameter("from", now)
                    .setParameter("to", now.plusDays(days))
                    .getResultStream()
                    .map(EventDTO::new)
                    .toList();
        }
    }

    @Override
    public EventDTO update(Long id, EventDTO dto) {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                em.getTransaction().begin();
                Event event = findOrThrow(em, id);
                if (dto.title() != null) event.setTitle(dto.title());
                if (dto.description() != null) event.setDescription(dto.description());
                if (dto.location() != null) event.setLocation(dto.location());
                if (dto.startTime() != null) event.setStartTime(dto.startTime());
                if (dto.endTime() != null) event.setEndTime(dto.endTime());
                em.getTransaction().commit();
                return new EventDTO(event);
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

    private Event findOrThrow(EntityManager em, Long id) {
        Event event = em.find(Event.class, id);
        if (event == null) {
            throw new ApiException(404, "Event med id " + id + " findes ikke");
        }
        return event;
    }
}
