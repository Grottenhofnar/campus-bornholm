package dk.campusbornholm.entities;

import dk.campusbornholm.config.AppClock;
import dk.campusbornholm.security.ISecurityUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User implements ISecurityUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_name")
    )
    private Set<Role> roles = new HashSet<>();

    public User(String username, String password) {
        this.username = username;
        setPassword(password);
    }

    public void setPassword(String password) {
        this.password = BCrypt.hashpw(password, BCrypt.gensalt());
    }

    @PrePersist
    private void onCreate() {
        this.createdAt = AppClock.now();
    }

    @Override
    public boolean verifyPassword(String pw) {
        if (pw == null || password == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(pw, password);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public Set<String> getRolesAsStrings() {
        return roles.stream().map(Role::getName).collect(Collectors.toSet());
    }

    @Override
    public void addRole(Role role) {
        roles.add(role);
        role.getUsers().add(this);
    }

    @Override
    public void removeRole(String roleName) {
        Iterator<Role> iterator = roles.iterator();
        while (iterator.hasNext()) {
            Role role = iterator.next();
            if (role.getName().equalsIgnoreCase(roleName)) {
                iterator.remove();
                role.getUsers().remove(this);
            }
        }
    }
}
