package dk.campusbornholm.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import dk.campusbornholm.entities.Role;
import dk.campusbornholm.entities.User;

import java.util.Set;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserDTO(
        String username,
        String password,
        Set<String> roles
) {
    public UserDTO(User user) {
        this(user.getUsername(),
                null,
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
    }
}
