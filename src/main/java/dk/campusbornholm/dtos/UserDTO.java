package dk.campusbornholm.dtos;

import dk.campusbornholm.entities.Role;
import dk.campusbornholm.entities.User;

import java.util.Set;
import java.util.stream.Collectors;

public record UserDTO(
        String username,
        Set<String> roles
) {
    public UserDTO(User user) {
        this(user.getUsername(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
    }
}
