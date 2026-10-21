package dk.campusbornholm.daos;

import dk.campusbornholm.dtos.UserDTO;
import dk.campusbornholm.entities.Role;
import dk.campusbornholm.entities.User;
import dk.campusbornholm.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;

import java.util.List;

public class UserDAO {
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    private final EntityManagerFactory emf;

    public UserDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public UserDTO create(UserDTO dto) {
        if (dto.username() == null || dto.username().isBlank()
                || dto.password() == null || dto.password().isBlank()) {
            throw new ApiException(400, "Brugernavn og password skal udfyldes");
        }
        try (EntityManager em = emf.createEntityManager()) {
            try {
                em.getTransaction().begin();

                Long existing = em.createQuery(
                                "SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                        .setParameter("username", dto.username())
                        .getSingleResult();
                if (existing > 0) {
                    throw new ApiException(409, "Brugernavnet '" + dto.username() + "' er optaget");
                }

                User user = new User(dto.username(), dto.password());
                user.addRole(getOrCreateRole(em, ROLE_USER));
                em.persist(user);

                em.getTransaction().commit();
                return new UserDTO(user);
            } catch (RuntimeException e) {
                DAOUtils.rollbackIfActive(em);
                throw e;
            }
        }
    }

    public UserDTO getByUsername(String username) {
        try (EntityManager em = emf.createEntityManager()) {
            return new UserDTO(DAOUtils.findUserOrThrow(em, username));
        }
    }

    public List<UserDTO> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles ORDER BY u.username", User.class)
                    .getResultStream()
                    .map(UserDTO::new)
                    .toList();
        }
    }

    public UserDTO addRole(String username, String roleName) {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                em.getTransaction().begin();
                User user = DAOUtils.findUserOrThrow(em, username);
                user.addRole(getOrCreateRole(em, roleName.toUpperCase()));
                em.getTransaction().commit();
                return new UserDTO(user);
            } catch (RuntimeException e) {
                DAOUtils.rollbackIfActive(em);
                throw e;
            }
        }
    }

    public void delete(String username) {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                em.getTransaction().begin();
                User user = DAOUtils.findUserOrThrow(em, username);
                user.getRoles().forEach(role -> role.getUsers().remove(user));
                user.getRoles().clear();
                em.remove(user);
                em.getTransaction().commit();
            } catch (ApiException e) {
                DAOUtils.rollbackIfActive(em);
                throw e;
            } catch (PersistenceException e) {
                DAOUtils.rollbackIfActive(em);
                throw new ApiException(409, "Brugeren '" + username + "' har stadig data og kan ikke slettes");
            }
        }
    }

    private Role getOrCreateRole(EntityManager em, String name) {
        Role role = em.find(Role.class, name);
        if (role == null) {
            role = new Role(name);
            em.persist(role);
        }
        return role;
    }
}
