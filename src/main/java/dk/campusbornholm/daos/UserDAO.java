package dk.campusbornholm.daos;

import dk.campusbornholm.dtos.UserDTO;
import dk.campusbornholm.entities.Role;
import dk.campusbornholm.entities.User;
import dk.campusbornholm.exceptions.ApiException;
import dk.campusbornholm.security.ISecurityDAO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceException;

import java.util.List;
import java.util.Optional;

public class UserDAO implements ISecurityDAO {
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    private static final String WRONG_LOGIN = "Forkert brugernavn eller password";

    private final EntityManagerFactory emf;

    public UserDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public UserDTO getVerifiedUser(String username, String password) {
        try (EntityManager em = emf.createEntityManager()) {
            User user = findByUsername(em, username)
                    .orElseThrow(() -> new ApiException(401, WRONG_LOGIN));
            if (!user.verifyPassword(password)) {
                throw new ApiException(401, WRONG_LOGIN);
            }
            return new UserDTO(user);
        }
    }

    @Override
    public UserDTO createUser(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new ApiException(400, "Brugernavn og password skal udfyldes");
        }
        try (EntityManager em = emf.createEntityManager()) {
            try {
                em.getTransaction().begin();
                if (findByUsername(em, username).isPresent()) {
                    throw new ApiException(409, "Brugernavnet '" + username + "' er optaget");
                }
                User user = new User(username, password);
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

    @Override
    public String createRole(String role) {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                em.getTransaction().begin();
                Role created = getOrCreateRole(em, role.toUpperCase());
                em.getTransaction().commit();
                return created.getName();
            } catch (RuntimeException e) {
                DAOUtils.rollbackIfActive(em);
                throw e;
            }
        }
    }

    @Override
    public UserDTO addUserRole(String username, String role) {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                em.getTransaction().begin();
                User user = DAOUtils.findUserOrThrow(em, username);
                user.addRole(getOrCreateRole(em, role.toUpperCase()));
                em.getTransaction().commit();
                return new UserDTO(user);
            } catch (RuntimeException e) {
                DAOUtils.rollbackIfActive(em);
                throw e;
            }
        }
    }

    public UserDTO createAdminIfMissing(String username, String password) {
        try (EntityManager em = emf.createEntityManager()) {
            if (findByUsername(em, username).isPresent()) {
                return addUserRole(username, ROLE_ADMIN);
            }
        }
        createUser(username, password);
        return addUserRole(username, ROLE_ADMIN);
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

    private Optional<User> findByUsername(EntityManager em, String username) {
        if (username == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
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
