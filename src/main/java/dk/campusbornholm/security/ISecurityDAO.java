package dk.campusbornholm.security;

import dk.campusbornholm.dtos.UserDTO;

public interface ISecurityDAO {

    UserDTO getVerifiedUser(String username, String password);

    UserDTO createUser(String username, String password);

    String createRole(String role);

    UserDTO addUserRole(String username, String role);
}
