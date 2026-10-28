package dk.campusbornholm.security;

import dk.bugelhartmann.ITokenSecurity;
import dk.bugelhartmann.TokenSecurity;
import dk.bugelhartmann.TokenVerificationException;
import dk.campusbornholm.config.AppProperties;
import dk.campusbornholm.dtos.LoginRequestDTO;
import dk.campusbornholm.dtos.TokenDTO;
import dk.campusbornholm.dtos.UserDTO;
import dk.campusbornholm.exceptions.ApiException;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Set;
import java.util.stream.Collectors;

public class SecurityController {

    private static final Logger log = LoggerFactory.getLogger(SecurityController.class);

    private static final String USER_ATTRIBUTE = "user";
    private static final String BEARER = "Bearer ";
    private static final String USERNAME_PATTERN = "[A-Za-z0-9_]{3,30}";
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final ISecurityDAO securityDAO;
    private final ITokenSecurity tokenSecurity = new TokenSecurity();

    public SecurityController(ISecurityDAO securityDAO) {
        this.securityDAO = securityDAO;
    }

    public void register(Context ctx) {
        LoginRequestDTO dto = ctx.bodyValidator(LoginRequestDTO.class)
                .check(d -> d.username() != null && d.username().matches(USERNAME_PATTERN),
                        "username skal være 3-30 tegn og må kun indeholde bogstaver (a-z), tal og _")
                .check(d -> d.password() != null && d.password().length() >= MIN_PASSWORD_LENGTH,
                        "password skal være mindst " + MIN_PASSWORD_LENGTH + " tegn")
                .get();

        UserDTO user = securityDAO.createUser(dto.username(), dto.password());
        ctx.status(HttpStatus.CREATED).json(tokenResponse(user));
    }

    public void login(Context ctx) {
        LoginRequestDTO dto = ctx.bodyValidator(LoginRequestDTO.class)
                .check(d -> d.username() != null && !d.username().isBlank(), "username skal udfyldes")
                .check(d -> d.password() != null && !d.password().isBlank(), "password skal udfyldes")
                .get();

        UserDTO user = securityDAO.getVerifiedUser(dto.username(), dto.password());
        ctx.status(HttpStatus.OK).json(tokenResponse(user));
    }

    public void me(Context ctx) {
        ctx.json(loggedInUser(ctx));
    }

    public void authenticate(Context ctx) {
        if (ctx.method() == HandlerType.OPTIONS) {
            return;
        }
        if (isOpenEndpoint(allowedRoles(ctx))) {
            return;
        }
        UserDTO user = verifyToken(getToken(ctx));
        ctx.attribute(USER_ATTRIBUTE, user);
    }

    public void authorize(Context ctx) {
        Set<String> allowedRoles = allowedRoles(ctx);
        if (isOpenEndpoint(allowedRoles)) {
            return;
        }
        UserDTO user = ctx.attribute(USER_ATTRIBUTE);
        if (user == null) {
            throw new ApiException(403, "Der blev ikke fundet en bruger i tokenet");
        }
        if (!userHasAllowedRole(user, allowedRoles)) {
            throw new ApiException(403, "Kræver en af rollerne " + allowedRoles + ", men brugeren har " + user.roles());
        }
    }

    public static UserDTO loggedInUser(Context ctx) {
        UserDTO user = ctx.attribute(USER_ATTRIBUTE);
        if (user == null) {
            throw new ApiException(401, "Du skal være logget ind");
        }
        return user;
    }

    public static String loggedInUsername(Context ctx) {
        return loggedInUser(ctx).username();
    }

    private TokenDTO tokenResponse(UserDTO user) {
        return new TokenDTO(createToken(user), user.username(), user.roles());
    }

    private String createToken(UserDTO user) {
        try {
            return tokenSecurity.createToken(
                    new dk.bugelhartmann.UserDTO(user.username(), user.roles()),
                    AppProperties.get("ISSUER"),
                    AppProperties.get("TOKEN_EXPIRE_TIME"),
                    AppProperties.get("SECRET_KEY"));
        } catch (Exception e) {
            log.error("Kunne ikke lave token", e);
            throw new ApiException(500, "Kunne ikke lave token");
        }
    }

    private UserDTO verifyToken(String token) {
        try {
            if (tokenSecurity.tokenIsValid(token, AppProperties.get("SECRET_KEY"))
                    && tokenSecurity.tokenNotExpired(token)) {
                dk.bugelhartmann.UserDTO user = tokenSecurity.getUserWithRolesFromToken(token);
                return new UserDTO(user.getUsername(), user.getRoles());
            }
        } catch (ParseException | TokenVerificationException e) {
            log.warn("Token kunne ikke verificeres: {}", e.getMessage());
        }
        throw new ApiException(401, "Ugyldigt eller udløbet token");
    }

    private static String getToken(Context ctx) {
        String header = ctx.header("Authorization");
        if (header == null || header.isBlank()) {
            throw new ApiException(401, "Authorization-header mangler");
        }
        if (!header.startsWith(BEARER) || header.length() <= BEARER.length()) {
            throw new ApiException(401, "Authorization-headeren skal have formen 'Bearer <token>'");
        }
        return header.substring(BEARER.length()).trim();
    }

    private static Set<String> allowedRoles(Context ctx) {
        return ctx.routeRoles().stream()
                .map(role -> role.toString().toUpperCase())
                .collect(Collectors.toSet());
    }

    private static boolean isOpenEndpoint(Set<String> allowedRoles) {
        return allowedRoles.isEmpty() || allowedRoles.contains(SecurityRoutes.Role.ANYONE.name());
    }

    private static boolean userHasAllowedRole(UserDTO user, Set<String> allowedRoles) {
        return user.roles().stream().anyMatch(role -> allowedRoles.contains(role.toUpperCase()));
    }
}
