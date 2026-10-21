package dk.campusbornholm.daos;

import dk.campusbornholm.entities.User;
import dk.campusbornholm.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

final class DAOUtils {
    private DAOUtils() {
    }

    static User findUserOrThrow(EntityManager em, String username) {
        if (username == null || username.isBlank()) {
            throw new ApiException(400, "Brugernavn mangler");
        }
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new ApiException(404, "Bruger '" + username + "' findes ikke");
        }
    }

    static void rollbackIfActive(EntityManager em) {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
    }
}
