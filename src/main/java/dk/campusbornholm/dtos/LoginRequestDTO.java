package dk.campusbornholm.dtos;

public record LoginRequestDTO(
        String username,
        String password
) {
}
