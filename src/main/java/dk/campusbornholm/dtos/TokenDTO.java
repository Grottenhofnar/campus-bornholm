package dk.campusbornholm.dtos;

import java.util.Set;

public record TokenDTO(
        String token,
        String username,
        Set<String> roles
) {
}
