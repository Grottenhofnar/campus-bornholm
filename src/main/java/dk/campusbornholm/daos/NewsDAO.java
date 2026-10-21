package dk.campusbornholm.daos;

import dk.campusbornholm.dtos.NewsDTO;
import dk.campusbornholm.entities.News;
import dk.campusbornholm.entities.User;
import dk.campusbornholm.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class NewsDAO implements IDAO<NewsDTO, Long> {
    private final EntityManagerFactory emf;

    public NewsDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public NewsDTO create(NewsDTO dto) {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                em.getTransaction().begin();
                User author = DAOUtils.findUserOrThrow(em, dto.authorName());
                News news = new News(dto.title(), dto.content(), author);
                em.persist(news);
                em.getTransaction().commit();
                return new NewsDTO(news);
            } catch (RuntimeException e) {
                DAOUtils.rollbackIfActive(em);
                throw e;
            }
        }
    }

    @Override
    public NewsDTO getById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return new NewsDTO(findOrThrow(em, id));
        }
    }

    @Override
    public List<NewsDTO> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT n FROM News n JOIN FETCH n.author ORDER BY n.createdAt DESC", News.class)
                    .getResultStream()
                    .map(NewsDTO::new)
                    .toList();
        }
    }

    public List<NewsDTO> getLatest(int limit) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT n FROM News n JOIN FETCH n.author ORDER BY n.createdAt DESC", News.class)
                    .setMaxResults(limit)
                    .getResultStream()
                    .map(NewsDTO::new)
                    .toList();
        }
    }

    @Override
    public NewsDTO update(Long id, NewsDTO dto) {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                em.getTransaction().begin();
                News news = findOrThrow(em, id);
                if (dto.title() != null) news.setTitle(dto.title());
                if (dto.content() != null) news.setContent(dto.content());
                em.getTransaction().commit();
                return new NewsDTO(news);
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

    private News findOrThrow(EntityManager em, Long id) {
        News news = em.find(News.class, id);
        if (news == null) {
            throw new ApiException(404, "Nyhed med id " + id + " findes ikke");
        }
        return news;
    }
}
